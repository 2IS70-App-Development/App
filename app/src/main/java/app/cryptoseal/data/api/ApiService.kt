package app.cryptoseal.data.api

import android.content.Context
import android.util.Base64
import android.util.Log
import app.cryptoseal.data.model.Activity
import app.cryptoseal.data.model.AuthResponse
import app.cryptoseal.data.model.Contact
import app.cryptoseal.data.model.ContactIdRequest
import app.cryptoseal.data.model.CreateOrderRequest
import app.cryptoseal.data.model.CreateScanRequest
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

object ApiService {
    private const val TAG = "ApiService"
    private const val BASE_URL = "http://10.0.2.2:8080"
    private val gson = Gson()
    private var sessionManager: SessionManager? = null

    fun initialize(context: Context) {
        sessionManager = SessionManager(context)
        authToken = sessionManager?.authToken
        currentUser = sessionManager?.currentUser
    }

    var authToken: String? = null
        private set

    var currentUser: User? = null
        private set

    private fun readResponse(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return stream?.bufferedReader()?.use { it.readText() } ?: ""
    }

    private fun parseError(response: String, defaultMessage: String): String {
        return try {
            gson.fromJson(response, ErrorResponse::class.java).error
        } catch (e: Exception) {
            defaultMessage
        }
    }

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

    private suspend fun fetchCurrentUser(email: String) {
        getUsers().onSuccess { users ->
            currentUser = users.find { it.email == email }
            sessionManager?.currentUser = currentUser
        }
    }

    fun logout() {
        authToken = null
        currentUser = null
        sessionManager?.clear()
    }

    fun isLoggedIn(): Boolean = (authToken ?: sessionManager?.authToken) != null

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

            val request = CreateScanRequest(orderId, photoBase64, condition, longitude, latitude, comment)
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

    suspend fun scanPackage(orderId: Int, photoBytes: ByteArray, condition: String, longitude: Float, latitude: Float, comment: String = ""): Result<Unit> {
        val photoBase64 = Base64.encodeToString(photoBytes, Base64.NO_WRAP)
        return createOrderScan(orderId, photoBase64, condition, longitude, latitude, comment)
    }

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
