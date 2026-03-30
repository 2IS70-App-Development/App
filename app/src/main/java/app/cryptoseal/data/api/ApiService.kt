package app.cryptoseal.data.api

import android.content.Context
import android.util.Base64
import android.util.Log
import app.cryptoseal.data.api.ApiService.authToken
import app.cryptoseal.data.api.ApiService.currentUser
import app.cryptoseal.data.model.Activity
import app.cryptoseal.data.model.AuthResponse
import app.cryptoseal.data.model.Contact
import app.cryptoseal.data.model.ContactIdRequest
import app.cryptoseal.data.model.CreateOrderRequest
import app.cryptoseal.data.model.CreateOrderScanRequest
import app.cryptoseal.data.model.ErrorResponse
import app.cryptoseal.data.model.LoginRequest
import app.cryptoseal.data.model.Order
import app.cryptoseal.data.model.Scan
import app.cryptoseal.data.model.SignupRequest
import app.cryptoseal.data.model.UpdateOrderStatusRequest
import app.cryptoseal.data.model.User
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Singleton object responsible for all network communication with the CryptoSeal backend.
 * It manages authentication tokens, user sessions, and provides high-level functions for
 * signup, login, order management, and contact operations.
 *
 * All functions are designed to be called from a coroutine and perform networking on [Dispatchers.IO].
 */
object ApiService {
    private const val TAG = "ApiService"
    private const val BASE_URL = "https://app.dev.libr.live"
    private val gson = Gson()
    private var sessionManager: SessionManager? = null

    /**
     * Initializes the ApiService with the application context.
     * This should be called once at application startup.
     * @param context The application context used by SessionManager for persistent storage.
     */
    fun initialize(context: Context) {
        sessionManager = SessionManager(context)
        authToken = sessionManager?.authToken
        currentUser = sessionManager?.currentUser
    }

    /**
     * The currently stored JWT authentication token.
     * When set, it is automatically added to all authenticated requests.
     */
    var authToken: String? = null
        private set

    /**
     * The user profile information of the currently logged-in user.
     */
    var currentUser: User? = null
        private set

    /**
     * Reads the response body from an [HttpURLConnection].
     * Handles both success (2xx) and error streams.
     * @param conn The open HttpURLConnection to read from.
     * @return The response body as a String, or an empty string if reading fails.
     */
    private fun readResponse(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return stream?.bufferedReader()?.use { it.readText() } ?: ""
    }

    /**
     * Parses a JSON error response string into a human-readable message.
     * Falls back to a default message if parsing fails.
     * @param response The JSON string containing error details.
     * @param defaultMessage The message to return if parsing fails.
     */
    private fun parseError(response: String, defaultMessage: String): String {
        return try {
            gson.fromJson(response, ErrorResponse::class.java).error
        } catch (e: Exception) {
            defaultMessage
        }
    }

    /**
     * Creates a new user account with the given email and password.
     * @return A [Result] containing the created [User] on success.
     */
    suspend fun signup(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/signup")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val request = SignupRequest(email, password)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val responseCode = conn.responseCode
            val response = readResponse(conn)
            
            if (responseCode in 200..299) {
                Result.success(gson.fromJson(response, User::class.java))
            } else {
                Result.failure(Exception(parseError(response, "Signup failed")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error during signup"))
        }
    }

    /**
     * Authenticates a user and retrieves a JWT token.
     * On successful login, it updates [authToken], persists it, and fetches the current user's profile.
     * @return A [Result] containing the JWT access token on success.
     */
    suspend fun login(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/jwt/create")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val request = LoginRequest(email, password)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                val authResponse = gson.fromJson(response, AuthResponse::class.java)
                authToken = authResponse.accessToken
                sessionManager?.authToken = authToken
                fetchCurrentUser(email)
                Result.success(authResponse.accessToken)
            } else {
                Result.failure(Exception(parseError(response, "Login failed")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error during login"))
        }
    }

    /**
     * Fetches the user profile for the given email from the list of all users.
     * Updates the local [currentUser] state and persists it.
     */
    private suspend fun fetchCurrentUser(email: String) {
        getUsers().onSuccess { users ->
            currentUser = users.find { it.email == email }
            sessionManager?.currentUser = currentUser
        }
    }

    /**
     * Clears all authentication state, including the token and user profile.
     * Effectively logs out the user and clears persistent session storage.
     */
    fun logout() {
        authToken = null
        currentUser = null
        sessionManager?.clear()
    }

    /**
     * Checks if a user is currently logged in based on the presence of an auth token.
     */
    fun isLoggedIn(): Boolean = (authToken ?: sessionManager?.authToken) != null

    /**
     * Prepares an [HttpURLConnection] with required authentication headers.
     * @param path The relative endpoint path (e.g., "/auth/orders").
     * @param method The HTTP method (GET, POST, PUT, DELETE).
     * @return An open connection with Authorization and Content-Type headers set.
     */
    private fun authenticatedConnection(path: String, method: String): HttpURLConnection {
        val currentToken = authToken ?: sessionManager?.authToken
        val url = URL("$BASE_URL$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method

        if (currentToken != null) {
            conn.setRequestProperty("Authorization", "Bearer $currentToken")
            Log.d(TAG, "Authenticated request to $path with Bearer token")
        } else {
            Log.w(TAG, "Attempting authenticated request to $path but token is NULL")
        }

        conn.setRequestProperty("Content-Type", "application/json")
        return conn
    }

    /**
     * Retrieves the list of all users in the system.
     * Requires authentication.
     */
    suspend fun getUsers(): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/users", "GET")
            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, Array<User>::class.java).toList())
            } else {
                val msg = if (responseCode == 401) "Unauthorized" else parseError(response, "Failed to fetch users")
                Result.failure(Exception(msg))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Retrieves detailed information for a specific user.
     * @param id The ID of the user to fetch.
     */
    suspend fun getUserDetails(id: Int): Result<User> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/users/details?id=$id", "GET")
            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, User::class.java))
            } else {
                Result.failure(Exception(parseError(response, "User not found")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Retrieves the list of all orders associated with the current user.
     * Requires authentication.
     */
    suspend fun getOrders(): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders", "GET")
            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, Array<Order>::class.java).toList())
            } else {
                Log.e(TAG, "getOrders failed: code=$responseCode, response=$response")
                val msg = if (responseCode == 401) "Unauthorized" else parseError(response, "Failed to fetch orders")
                Result.failure(Exception(msg))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Retrieves detailed information for a specific order.
     * @param id The ID of the order to fetch.
     */
    suspend fun getOrderDetails(id: Int): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders/details?id=$id", "GET")
            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, Order::class.java))
            } else {
                Result.failure(Exception(parseError(response, "Order not found")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Creates a new package order.
     * @param receiverId The user ID of the package recipient.
     * @param name The name or title of the package.
     * @param meta Optional metadata associated with the order.
     * @param comment An optional comment for the order.
     * @param photo Optional Base64 encoded photo of the package.
     */
    suspend fun createOrder(
        receiverId: Int,
        name: String,
        meta: String = "",
        comment: String = "",
        photo: String? = null
    ): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders", "POST")
            conn.doOutput = true

            val request = CreateOrderRequest(receiverId, name, meta, comment, photo)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode in 200..299) {
                Result.success(gson.fromJson(response, Order::class.java))
            } else {
                Result.failure(Exception(parseError(response, "Failed to create order")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Updates the status of an existing order (e.g., from 'SENT' to 'DELIVERED').
     */
    suspend fun updateOrderStatus(orderId: Int, status: String): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders/status", "PUT")
            conn.doOutput = true

            val request = UpdateOrderStatusRequest(orderId, status)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, Order::class.java))
            } else {
                Result.failure(Exception(parseError(response, "Failed to update order status")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Fetches the history of scans (location/condition updates) for a specific order.
     */
    suspend fun getOrderScans(orderId: Int): Result<List<Scan>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders/scans?order_id=$orderId", "GET")
            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, Array<Scan>::class.java).toList())
            } else {
                Result.failure(Exception(parseError(response, "Failed to fetch scans")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Creates a new scan entry for an order, typically recorded when a package changes hands.
     * @param photoBase64 Base64 encoded image taken during the scan.
     * @param condition Description of the package condition (e.g., "Good", "Damaged").
     * @param longitude GPS longitude coordinate.
     * @param latitude GPS latitude coordinate.
     */
    suspend fun createOrderScan(
        orderId: Int,
        photoBase64: String,
        condition: String,
        longitude: Float,
        latitude: Float,
        comment: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders/scan", "POST")
            conn.doOutput = true

            val request = CreateOrderScanRequest(
                orderId,
                photoBase64,
                condition,
                longitude,
                latitude,
                comment
            )
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(response, "Failed to create scan")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Helper function to scan a package using raw byte array photo data.
     * Automatically encodes the photo to Base64 before sending.
     */
    suspend fun scanPackage(orderId: Int, photoBytes: ByteArray, condition: String, longitude: Float, latitude: Float, comment: String = ""): Result<Unit> {
        val photoBase64 = Base64.encodeToString(photoBytes, Base64.NO_WRAP)
        return createOrderScan(orderId, photoBase64, condition, longitude, latitude, comment)
    }

    /**
     * Retrieves the current user's contact list.
     */
    suspend fun getContacts(): Result<List<Contact>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/contacts", "GET")
            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, Array<Contact>::class.java).toList())
            } else {
                Result.failure(Exception(parseError(response, "Failed to fetch contacts")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Adds a user to the current user's contacts.
     * @param contactId The ID of the user to be added.
     */
    suspend fun addContact(contactId: Int): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/contacts", "POST")
            conn.doOutput = true

            val request = ContactIdRequest(contactId)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode in 200..299) {
                Result.success(gson.fromJson(response, Contact::class.java))
            } else {
                Result.failure(Exception(parseError(response, "Failed to add contact")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Removes a user from the current user's contact list.
     */
    suspend fun removeContact(contactId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/contacts", "DELETE")
            conn.doOutput = true

            val request = ContactIdRequest(contactId)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(response, "Failed to remove contact")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    /**
     * Retrieves a list of recent activities/notifications for the current user.
     */
    suspend fun getActivities(): Result<List<Activity>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/activities", "GET")
            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, Array<Activity>::class.java).toList())
            } else {
                val msg = if (responseCode == 401) "Unauthorized" else parseError(
                    response,
                    "Failed to fetch activities"
                )
                Result.failure(Exception(msg))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }
}
