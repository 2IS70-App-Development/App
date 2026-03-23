package app.cryptoseal.tabs.creator

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cryptoseal.data.api.ApiService
import app.cryptoseal.data.model.Order
import app.cryptoseal.util.QRUtils
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
                        QRUtils.generateQrBitmap(order.id.toString())
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
}
