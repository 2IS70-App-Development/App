package app.cryptoseal.data.api

import android.util.Log
import app.cryptoseal.data.model.CreateOrderRequest
import app.cryptoseal.data.model.Order
import app.cryptoseal.data.model.UpdateOrderStatusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import javax.net.ssl.HttpsURLConnection

/**
 * Handles order-related API calls.
 */
class OrderApiService : BaseApiService() {

    suspend fun getOrders(): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/orders", "GET")
            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, Array<Order>::class.java).toList())
            } else {
                Log.e(TAG, "getOrders failed: code=$responseCode, response=$response")
                val msg = if (responseCode == 401) "Unauthorized" else parseError(
                    response,
                    "Failed to fetch orders"
                )
                Result.failure(Exception(msg))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    suspend fun getOrderDetails(id: Int): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val urlPath = "/auth/orders/details?id=$id"
            val conn = authenticatedConnection(urlPath, "GET")
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

    suspend fun updateOrderStatus(orderId: Int, status: String): Result<Order> =
        withContext(Dispatchers.IO) {
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
}
