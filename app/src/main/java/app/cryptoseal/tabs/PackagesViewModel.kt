package app.cryptoseal.tabs

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cryptoseal.data.api.ApiService
import app.cryptoseal.tabs.packages.PackageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PackagesViewModel : ViewModel() {

    private val _allPackages = MutableStateFlow<List<PackageItem>>(emptyList())
    val allPackages: StateFlow<List<PackageItem>> = _allPackages.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refreshPackages()
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
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

    fun addPackage(packageItem: PackageItem) {
        _allPackages.value = _allPackages.value + packageItem
    }
}
