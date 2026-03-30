package app.cryptoseal.data.api

import app.cryptoseal.data.model.AuthResponse
import app.cryptoseal.data.model.LoginRequest
import app.cryptoseal.data.model.SignupRequest
import app.cryptoseal.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Handles authentication-related API calls: Signup and Login.
 */
class AuthApiService : BaseApiService() {

    suspend fun signup(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
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

    suspend fun login(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
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
                    Result.success(authResponse.accessToken)
                } else {
                    Result.failure(Exception(parseError(response, "Login failed")))
                }.also { conn.disconnect() }
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "Network error during login"))
            }
        }
}
