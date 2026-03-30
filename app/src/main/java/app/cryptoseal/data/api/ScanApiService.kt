package app.cryptoseal.data.api

import android.util.Base64
import app.cryptoseal.data.model.CreateOrderScanRequest
import app.cryptoseal.data.model.Scan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import javax.net.ssl.HttpsURLConnection

/**
 * Handles scan-related API calls.
 */
class ScanApiService : BaseApiService() {

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

    suspend fun scanPackage(
        orderId: Int,
        photoBytes: ByteArray,
        condition: String,
        longitude: Float,
        latitude: Float,
        comment: String = ""
    ): Result<Unit> {
        val photoBase64 = Base64.encodeToString(photoBytes, Base64.NO_WRAP)
        return createOrderScan(orderId, photoBase64, condition, longitude, latitude, comment)
    }
}
