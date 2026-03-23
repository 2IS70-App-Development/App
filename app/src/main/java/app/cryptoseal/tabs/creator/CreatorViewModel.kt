package app.cryptoseal.tabs.creator

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cryptoseal.data.api.ApiService
import app.cryptoseal.data.model.Order
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserDisplay(
    val id: Int,
    val email: String
)

sealed class CreateOrderResult {
    data class Success(val order: Order, val qrBitmap: Bitmap) : CreateOrderResult()
    data class Error(val message: String) : CreateOrderResult()
}

class CreatorViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<UserDisplay>>(emptyList())
    val users: StateFlow<List<UserDisplay>> = _users.asStateFlow()

    private val _isLoadingUsers = MutableStateFlow(false)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers.asStateFlow()

    private val _createOrderResult = MutableStateFlow<CreateOrderResult?>(null)
    val createOrderResult: StateFlow<CreateOrderResult?> = _createOrderResult.asStateFlow()

    private val _isCreatingOrder = MutableStateFlow(false)
    val isCreatingOrder: StateFlow<Boolean> = _isCreatingOrder.asStateFlow()

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            ApiService.getUsers().fold(
                onSuccess = { userList ->
                    val currentUser = ApiService.currentUser
                    _users.value = userList
                        .filter { it.id != currentUser?.id }
                        .map { UserDisplay(it.id, it.email) }
                },
                onFailure = { _users.value = emptyList() }
            )
            _isLoadingUsers.value = false
        }
    }

    fun createOrder(
        receiverId: Int,
        name: String,
        meta: String,
        comment: String,
        photoBase64: String?
    ) {
        viewModelScope.launch {
            _isCreatingOrder.value = true
            _createOrderResult.value = null

            ApiService.createOrder(receiverId, name, meta, comment, photoBase64).fold(
                onSuccess = { order ->
                    val qrBitmap = withContext(Dispatchers.Default) {
                        generateQrBitmap(order.id.toString())
                    }
                    _createOrderResult.value = CreateOrderResult.Success(order, qrBitmap)
                },
                onFailure = { e ->
                    _createOrderResult.value = CreateOrderResult.Error(e.message ?: "Failed to create order")
                }
            )
            _isCreatingOrder.value = false
        }
    }

    fun clearCreateResult() {
        _createOrderResult.value = null
    }

    private fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
