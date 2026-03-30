package app.cryptoseal.data.api

import app.cryptoseal.data.model.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.net.ssl.HttpsURLConnection

/**
 * Handles activity-related API calls.
 */
class ActivityApiService : BaseApiService() {

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
