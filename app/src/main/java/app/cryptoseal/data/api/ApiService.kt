package app.cryptoseal.data.api

import android.util.Base64
import app.cryptoseal.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ApiService {
    private const val BASE_URL = "http://192.168.240.1:8089"
    private val gson = Gson()

    var authToken: String? = null
        private set

    var currentUser: User? = null
        private set

    suspend fun signup(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/signup")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val request = SignupRequest(email, password)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK, HttpsURLConnection.HTTP_CREATED -> {
                    Result.success(gson.fromJson(response, User::class.java))
                }
                else -> {
                    val error = try {
                        gson.fromJson(response, ErrorResponse::class.java).error
                    } catch (e: Exception) {
                        "Signup failed"
                    }
                    Result.failure(Exception(error))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
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

            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK -> {
                    val authResponse = gson.fromJson(response, AuthResponse::class.java)
                    authToken = authResponse.accessToken
                    fetchCurrentUser(email)
                    Result.success(authResponse.accessToken)
                }
                else -> {
                    val error = try {
                        gson.fromJson(response, ErrorResponse::class.java).error
                    } catch (e: Exception) {
                        "Login failed"
                    }
                    Result.failure(Exception(error))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchCurrentUser(email: String) {
        getUsers().onSuccess { users ->
            currentUser = users.find { it.email == email }
        }
    }

    fun logout() {
        authToken = null
        currentUser = null
    }

    fun isLoggedIn(): Boolean = authToken != null

    private fun authenticatedConnection(path: String, method: String): HttpURLConnection {
        val url = URL("$BASE_URL$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Authorization", "Bearer $authToken")
        conn.setRequestProperty("Content-Type", "application/json")
        return conn
    }

    suspend fun getUsers(): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/users", "GET")
            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK -> {
                    Result.success(gson.fromJson(response, Array<User>::class.java).toList())
                }
                HttpsURLConnection.HTTP_UNAUTHORIZED -> {
                    Result.failure(Exception("Unauthorized"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch users"))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserDetails(id: Int): Result<User> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/users/details?id=$id", "GET")
            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK -> {
                    Result.success(gson.fromJson(response, User::class.java))
                }
                else -> {
                    val error = try {
                        gson.fromJson(response, ErrorResponse::class.java).error
                    } catch (e: Exception) {
                        "User not found"
                    }
                    Result.failure(Exception(error))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrders(): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders", "GET")
            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK -> {
                    Result.success(gson.fromJson(response, Array<Order>::class.java).toList())
                }
                HttpsURLConnection.HTTP_UNAUTHORIZED -> {
                    Result.failure(Exception("Unauthorized"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch orders"))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderDetails(id: Int): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders/details?id=$id", "GET")
            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK -> {
                    Result.success(gson.fromJson(response, Order::class.java))
                }
                else -> {
                    val error = try {
                        gson.fromJson(response, ErrorResponse::class.java).error
                    } catch (e: Exception) {
                        "Order not found"
                    }
                    Result.failure(Exception(error))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrder(receiverId: Int, name: String, meta: String = "", comment: String = ""): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders", "POST")
            conn.doOutput = true

            val request = CreateOrderRequest(receiverId, name, meta, comment)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK, HttpsURLConnection.HTTP_CREATED -> {
                    Result.success(gson.fromJson(response, Order::class.java))
                }
                HttpsURLConnection.HTTP_UNAUTHORIZED -> {
                    Result.failure(Exception("Unauthorized"))
                }
                else -> {
                    val error = try {
                        gson.fromJson(response, ErrorResponse::class.java).error
                    } catch (e: Exception) {
                        "Failed to create order"
                    }
                    Result.failure(Exception(error))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: Int, status: String): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders/status", "PUT")
            conn.doOutput = true

            val request = UpdateOrderStatusRequest(orderId, status)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK -> {
                    Result.success(gson.fromJson(response, Order::class.java))
                }
                else -> {
                    val error = try {
                        gson.fromJson(response, ErrorResponse::class.java).error
                    } catch (e: Exception) {
                        "Failed to update order status"
                    }
                    Result.failure(Exception(error))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderScans(orderId: Int): Result<List<Scan>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders/scans?order_id=$orderId", "GET")
            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK -> {
                    Result.success(gson.fromJson(response, Array<Scan>::class.java).toList())
                }
                HttpsURLConnection.HTTP_UNAUTHORIZED -> {
                    Result.failure(Exception("Unauthorized"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch scans"))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
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

            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK, HttpsURLConnection.HTTP_CREATED -> {
                    Result.success(Unit)
                }
                HttpsURLConnection.HTTP_UNAUTHORIZED -> {
                    Result.failure(Exception("Unauthorized"))
                }
                else -> {
                    val response = conn.inputStream.bufferedReader().readText()
                    val error = try {
                        gson.fromJson(response, ErrorResponse::class.java).error
                    } catch (e: Exception) {
                        "Failed to create scan"
                    }
                    Result.failure(Exception(error))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanPackage(orderId: Int, photoBytes: ByteArray, condition: String, longitude: Float, latitude: Float, comment: String = ""): Result<Unit> {
        val photoBase64 = Base64.encodeToString(photoBytes, Base64.NO_WRAP)
        return createOrderScan(orderId, photoBase64, condition, longitude, latitude, comment)
    }

    suspend fun getContacts(): Result<List<Contact>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/contacts", "GET")
            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK -> {
                    Result.success(gson.fromJson(response, Array<Contact>::class.java).toList())
                }
                HttpsURLConnection.HTTP_UNAUTHORIZED -> {
                    Result.failure(Exception("Unauthorized"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch contacts"))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addContact(contactId: Int): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/contacts", "POST")
            conn.doOutput = true

            val request = ContactIdRequest(contactId)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val response = conn.inputStream.bufferedReader().readText()
            when (conn.responseCode) {
                HttpsURLConnection.HTTP_OK, HttpsURLConnection.HTTP_CREATED -> {
                    Result.success(gson.fromJson(response, Contact::class.java))
                }
                HttpsURLConnection.HTTP_UNAUTHORIZED -> {
                    Result.failure(Exception("Unauthorized"))
                }
                else -> {
                    val error = try {
                        gson.fromJson(response, ErrorResponse::class.java).error
                    } catch (e: Exception) {
                        "Failed to add contact"
                    }
                    Result.failure(Exception(error))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeContact(contactId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/contacts", "DELETE")
            conn.doOutput = true

            val request = ContactIdRequest(contactId)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            when (conn.responseCode) {
                HttpsURLConnection.HTTP_NO_CONTENT, HttpsURLConnection.HTTP_OK -> {
                    Result.success(Unit)
                }
                HttpsURLConnection.HTTP_UNAUTHORIZED -> {
                    Result.failure(Exception("Unauthorized"))
                }
                else -> {
                    val response = conn.inputStream.bufferedReader().readText()
                    val error = try {
                        gson.fromJson(response, ErrorResponse::class.java).error
                    } catch (e: Exception) {
                        "Failed to remove contact"
                    }
                    Result.failure(Exception(error))
                }
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
