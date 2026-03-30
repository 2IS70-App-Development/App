package app.cryptoseal.data.api

import android.util.Log
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL

/**
 * Common networking logic shared across specialized API services.
 */
abstract class BaseApiService {
    protected val BASE_URL = "https://app.dev.libr.live"
    protected val gson = Gson()
    protected val TAG = "ApiService"

    protected fun readResponse(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return stream?.bufferedReader()?.use { it.readText() } ?: ""
    }

    protected fun parseError(response: String, defaultMessage: String): String {
        return try {
            gson.fromJson(response, app.cryptoseal.data.model.ErrorResponse::class.java).error
        } catch (e: Exception) {
            defaultMessage
        }
    }

    protected fun authenticatedConnection(path: String, method: String): HttpURLConnection {
        val currentToken = ApiService.authToken
        val url = URL("$BASE_URL$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method

        if (currentToken != null) {
            conn.setRequestProperty("Authorization", "Bearer $currentToken")
        } else {
            Log.w(TAG, "Attempting authenticated request to $path but token is NULL")
        }

        conn.setRequestProperty("Content-Type", "application/json")
        return conn
    }
}
