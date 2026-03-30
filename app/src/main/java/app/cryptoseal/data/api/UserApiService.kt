package app.cryptoseal.data.api

import app.cryptoseal.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.net.ssl.HttpsURLConnection

/**
 * Handles user-related API calls.
 */
class UserApiService : BaseApiService() {

    suspend fun getUsers(): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/users", "GET")
            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, Array<User>::class.java).toList())
            } else {
                val msg = if (responseCode == 401) "Unauthorized" else parseError(
                    response,
                    "Failed to fetch users"
                )
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
}
