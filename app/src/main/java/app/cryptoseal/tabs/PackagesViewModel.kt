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
 * ViewModel responsible for managing package-related data and states within the application.
 * It handles fetching orders, managing scans, updating statuses, and generating QR codes.
 * This class serves as the bridge between the UI (PackagesTab, ScannerTab) and the data layer (ApiService).
 */
class PackagesViewModel : ViewModel() {

    // Internal mutable state flow for the list of all packages.
    private val _allPackages = MutableStateFlow<List<PackageItem>>(emptyList())

    /**
     * Publicly exposed state flow for observing the list of all packages.
     * UI components should collect from this flow to display package information.
     */
    val allPackages: StateFlow<List<PackageItem>> = _allPackages.asStateFlow()

    // Internal state flow to track the currently selected tab index in the Packages screen.
    private val _selectedTab = MutableStateFlow(0)

    /**
     * Publicly exposed state flow for the selected tab index.
     */
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Internal state flow to track the loading state of general package operations.
    private val _isLoading = MutableStateFlow(false)

    /**
     * Publicly exposed state flow indicating if a loading operation is in progress.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Internal state flow for holding any error messages encountered during data operations.
    private val _error = MutableStateFlow<String?>(null)

    /**
     * Publicly exposed state flow for error messages. UI can show snackbars or dialogs based on this.
     */
    val error: StateFlow<String?> = _error.asStateFlow()

    // Scans state: Holds the history of scans for a selected package.
    private val _scans = MutableStateFlow<List<Scan>>(emptyList())

    /**
     * Publicly exposed state flow for the list of scans associated with a specific package.
     */
    val scans: StateFlow<List<Scan>> = _scans.asStateFlow()

    // Internal state flow to track if scans are currently being loaded from the API.
    private val _isScansLoading = MutableStateFlow(false)

    /**
     * Publicly exposed state flow for the loading state of scan history.
     */
    val isScansLoading: StateFlow<Boolean> = _isScansLoading.asStateFlow()

    // Users for name mapping: Caches the list of users to resolve sender/receiver names.
    private val _users = MutableStateFlow<List<User>>(emptyList())

    /**
     * Publicly exposed state flow for the list of system users.
     */
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // Shared flow for one-time events like toast notifications.
    private val _toastMessage = MutableSharedFlow<String>()

    /**
     * Publicly exposed shared flow for toast messages that should only be shown once.
     */
    val toastMessage = _toastMessage.asSharedFlow()

    /**
     * Initializes the ViewModel by refreshing the package list and fetching the list of users.
     */
    init {
        refreshPackages()
        fetchUsers()
    }

    /**
     * Updates the currently selected tab index.
     * @param index The index of the tab to be selected.
     */
    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    /**
     * Fetches the list of all users from the API.
     * This is used for mapping user IDs to readable names in the UI.
     */
    private fun fetchUsers() {
        viewModelScope.launch {
            ApiService.getUsers().onSuccess { userList ->
                _users.value = userList
            }
        }
    }

    /**
     * Refreshes the list of packages (orders) from the server.
     * It maps the API order model to the UI PackageItem model, determining if the current user is the sender.
     */
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

                    // Map Order domain objects to PackageItem UI objects.
                    _allPackages.value = orders.map { order ->
                        // A package is "sent" by the user if their ID matches the senderId.
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

    /**
     * Fetches the scan history for a specific order.
     * @param orderId The unique identifier of the order.
     */
    fun fetchScans(orderId: Int) {
        viewModelScope.launch {
            _isScansLoading.value = true
            _scans.value = emptyList() // Clear previous scans while loading.
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

    /**
     * Updates the status of an existing order (e.g., marking it as DELIVERED).
     * @param orderId The string representation of the order ID.
     * @param newStatus The new status string to be applied.
     */
    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val idInt = orderId.toIntOrNull() ?: return@launch
            ApiService.updateOrderStatus(idInt, newStatus)
                .onSuccess {
                    _toastMessage.emit("Package marked as $newStatus")
                    // Refresh the lists to reflect changes in the UI.
                    refreshPackages()
                    fetchScans(idInt)
                }
                .onFailure {
                    _toastMessage.emit("Failed to update status: ${it.message}")
                }
        }
    }

    /**
     * Generates a QR code bitmap for the given content string.
     * This operation is performed on a background (Default) dispatcher.
     * @param content The string content to be encoded in the QR code.
     * @return A Bitmap representing the generated QR code.
     */
    suspend fun generateQrCode(content: String): Bitmap = withContext(Dispatchers.Default) {
        QRUtils.generateQrBitmap(content)
    }

    /**
     * Manually adds a package item to the internal list.
     * This can be used for optimistic UI updates or local-only additions.
     * @param packageItem The package item to be added.
     */
    fun addPackage(packageItem: PackageItem) {
        _allPackages.value = _allPackages.value + packageItem
    }
}
