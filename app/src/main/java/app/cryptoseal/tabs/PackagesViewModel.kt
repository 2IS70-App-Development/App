package app.cryptoseal.tabs

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cryptoseal.data.api.ApiService
import app.cryptoseal.data.model.Scan
import app.cryptoseal.data.model.User
import app.cryptoseal.tabs.packages.PackageItem
import app.cryptoseal.util.QRUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PackagesViewModel : ViewModel() {

    private val _allPackages = MutableStateFlow<List<PackageItem>>(emptyList())
    val allPackages: StateFlow<List<PackageItem>> = _allPackages.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Scans state
    private val _scans = MutableStateFlow<List<Scan>>(emptyList())
    val scans: StateFlow<List<Scan>> = _scans.asStateFlow()

    private val _isScansLoading = MutableStateFlow(false)
    val isScansLoading: StateFlow<Boolean> = _isScansLoading.asStateFlow()

    // Users for name mapping
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    init {
        refreshPackages()
        fetchUsers()
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            ApiService.getUsers().onSuccess { userList ->
                _users.value = userList
            }
        }
    }

    fun refreshPackages() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("PackagesViewModel", "Refreshing packages...")
            ApiService.getOrders()
                .onSuccess { orders ->
                    val currentUser = ApiService.currentUser
                    Log.d(
                        "PackagesViewModel",
                        "Fetched ${orders.size} orders. CurrentUser ID: ${currentUser?.id}"
                    )

                    _allPackages.value = orders.map { order ->
                        val isSent = order.senderId == currentUser?.id
                        Log.d(
                            "PackagesViewModel",
                            "Order ${order.id}: sender=${order.senderId}, isSentByMe=$isSent"
                        )
                        PackageItem(
                            id = order.id.toString(),
                            name = order.name,
                            status = order.status,
                            isSentByMe = isSent
                        )
                    }
                }
                .onFailure {
                    Log.e("PackagesViewModel", "Error fetching packages", it)
                    _error.value = it.message ?: "Failed to fetch packages"
                }
            _isLoading.value = false
        }
    }

    fun fetchScans(orderId: Int) {
        viewModelScope.launch {
            _isScansLoading.value = true
            _scans.value = emptyList()
            ApiService.getOrderScans(orderId)
                .onSuccess { scanList ->
                    _scans.value = scanList
                }
                .onFailure {
                    Log.e("PackagesViewModel", "Error fetching scans for order $orderId", it)
                }
            _isScansLoading.value = false
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val idInt = orderId.toIntOrNull() ?: return@launch
            ApiService.updateOrderStatus(idInt, newStatus)
                .onSuccess {
                    _toastMessage.emit("Package marked as $newStatus")
                    refreshPackages()
                    fetchScans(idInt)
                }
                .onFailure {
                    _toastMessage.emit("Failed to update status: ${it.message}")
                }
        }
    }

    suspend fun generateQrCode(content: String): Bitmap = withContext(Dispatchers.Default) {
        QRUtils.generateQrBitmap(content)
    }

    fun addPackage(packageItem: PackageItem) {
        _allPackages.value = _allPackages.value + packageItem
    }
}
