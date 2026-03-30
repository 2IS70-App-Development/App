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

/**
 * Simplified user model used for display and selection in the Creator UI.
 */
data class UserDisplay(
    val id: Int,
    val email: String
)

/**
 * Represents the result of an order creation attempt.
 */
sealed class CreateOrderResult {
    /**
     * Order was successfully created on the server and a QR code was generated locally.
     * @property order The successfully created [Order] object.
     * @property qrBitmap The generated QR code representing the order ID.
     */
    data class Success(val order: Order, val qrBitmap: Bitmap) : CreateOrderResult()

    /**
     * Order creation failed.
     * @property message A human-readable error message.
     */
    data class Error(val message: String) : CreateOrderResult()
}

/**
 * ViewModel for the "Creator" tab, handling the logic for creating new shipment orders.
 * It manages user lookup for the receiver field and the asynchronous creation process.
 */
class CreatorViewModel : ViewModel() {

    // List of users that can be selected as receivers (excluding the current user).
    private val _users = MutableStateFlow<List<UserDisplay>>(emptyList())
    val users: StateFlow<List<UserDisplay>> = _users.asStateFlow()

    // Loading state for fetching the user list.
    private val _isLoadingUsers = MutableStateFlow(false)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers.asStateFlow()

    // Holds the outcome of the latest order creation attempt.
    private val _createOrderResult = MutableStateFlow<CreateOrderResult?>(null)
    val createOrderResult: StateFlow<CreateOrderResult?> = _createOrderResult.asStateFlow()

    // Loading state for the order creation network request.
    private val _isCreatingOrder = MutableStateFlow(false)
    val isCreatingOrder: StateFlow<Boolean> = _isCreatingOrder.asStateFlow()

    /**
     * Fetches all users from the system and filters out the currently logged-in user.
     * This list is used to populate the receiver selection dropdown.
     */
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

    /**
     * Submits a new order to the backend and generates a QR code upon success.
     *
     * @param receiverId The user ID of the intended recipient.
     * @param name The descriptive name of the shipment.
     * @param meta Additional metadata string.
     * @param comment A comment or description for the order.
     * @param photoBase64 An optional Base64 encoded image of the package.
     */
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
                    // Generate QR code on a background thread as it can be computationally intensive.
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

    /**
     * Resets the creation result state, effectively dismissing any success or error UI.
     */
    fun clearCreateResult() {
        _createOrderResult.value = null
    }
}
