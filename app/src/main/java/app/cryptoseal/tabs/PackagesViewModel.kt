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

/**
 * PackagesViewModel: Central state manager for the Packages and Order Detail screens.
 * 
 * This ViewModel acts as a mediator between the raw data from [ApiService] and the 
 * presentation-optimized models used by the Compose UI. It manages the lifecycle 
 * of package lists, scan logs, and user name resolution.
 *
 * Key Responsibilities:
 * 1. Fetching and filtering orders based on the current user's role (Sender vs Receiver).
 * 2. Caching a list of system users for ID-to-email mapping in the UI.
 * 3. Handling asynchronous updates like changing an order's status.
 * 4. Generating QR code bitmaps on background threads.
 */
class PackagesViewModel : ViewModel() {

    // --- State Streams (Mutable Internal, Read-only External) ---

    // Master list of all packages associated with the user.
    private val _allPackages = MutableStateFlow<List<PackageItem>>(emptyList())
    val allPackages: StateFlow<List<PackageItem>> = _allPackages.asStateFlow()

    // Tracks which tab is selected in the Packages view: 0 for "Sent", 1 for "Received".
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // General loading state for the initial list fetch.
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state for handling network failures in the main list.
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // History of tracking scans for the currently inspected package.
    private val _scans = MutableStateFlow<List<Scan>>(emptyList())
    val scans: StateFlow<List<Scan>> = _scans.asStateFlow()

    // Loading state specifically for fetching scan history.
    private val _isScansLoading = MutableStateFlow(false)
    val isScansLoading: StateFlow<Boolean> = _isScansLoading.asStateFlow()

    // Cached list of all users to allow displaying emails instead of numeric IDs.
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // Shared flow for one-time events like Toast notifications (navigation, success msgs).
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    /**
     * Initialization block: Triggered when the Dashboard/Packages tab is first created.
     */
    init {
        refreshPackages()
        fetchUsers()
    }

    /**
     * Switches the filter context (Sent vs Received).
     * @param index 0 for Sent, 1 for Received.
     */
    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    /**
     * Fetches the global list of users to facilitate name resolution in lists and details.
     */
    private fun fetchUsers() {
        viewModelScope.launch {
            ApiService.getUsers().onSuccess { userList ->
                _users.value = userList
            }
        }
    }

    /**
     * Refreshes the package list from the backend.
     * 
     * It transforms the domain [Order] objects into UI-ready [PackageItem] objects, 
     * explicitly flagging which packages were initiated by the current user.
     */
    fun refreshPackages() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            ApiService.getOrders()
                .onSuccess { orders ->
                    val currentUser = ApiService.currentUser

                    // Map API orders to the UI data class
                    _allPackages.value = orders.map { order ->
                        // Determine directionality: Am I the one who sent this?
                        val isSent = order.senderId == currentUser?.id
                        PackageItem(
                            id = order.id.toString(),
                            name = order.name,
                            status = order.status,
                            isSentByMe = isSent
                        )
                    }
                }
                .onFailure {
                    _error.value = it.message ?: "Failed to fetch packages"
                }
            _isLoading.value = false
        }
    }

    /**
     * Fetches the chronological history of scans for a specific package.
     * 
     * @param orderId The numeric ID of the package.
     */
    fun fetchScans(orderId: Int) {
        viewModelScope.launch {
            _isScansLoading.value = true
            _scans.value = emptyList() // Reset list to show loading state clearly
            ApiService.getOrderScans(orderId)
                .onSuccess { scanList ->
                    // The server returns scans; the UI handles the vertical timeline.
                    _scans.value = scanList
                }
                .onFailure {
                    Log.e("PackagesViewModel", "Error fetching scans for order $orderId", it)
                }
            _isScansLoading.value = false
        }
    }

    /**
     * Updates an order's status (e.g., confirming delivery or cancelling).
     * 
     * @param orderId The ID of the order to modify.
     * @param newStatus The target status string.
     */
    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val idInt = orderId.toIntOrNull() ?: return@launch
            ApiService.updateOrderStatus(idInt, newStatus)
                .onSuccess {
                    _toastMessage.emit("Package marked as $newStatus")
                    // Trigger a list refresh to reflect the new status in the UI.
                    refreshPackages()
                    // Refresh scans to show the final delivery event in the timeline.
                    fetchScans(idInt)
                }
                .onFailure {
                    _toastMessage.emit("Failed to update status: ${it.message}")
                }
        }
    }

    /**
     * Generates a QR Code on a background dispatcher to avoid UI jank.
     * 
     * @param content The text to encode (usually the Order ID).
     */
    suspend fun generateQrCode(content: String): Bitmap = withContext(Dispatchers.Default) {
        QRUtils.generateQrBitmap(content)
    }

    /**
     * Local helper to add a package item manually (e.g. for optimistic UI updates).
     */
    fun addPackage(packageItem: PackageItem) {
        _allPackages.value = _allPackages.value + packageItem
    }
}
