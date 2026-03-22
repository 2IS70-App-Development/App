package app.cryptoseal.tabs

import androidx.lifecycle.ViewModel
import app.cryptoseal.tabs.packages.PackageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PackagesViewModel : ViewModel() {

    // --- State for the Unified List ---
    private val _allPackages = MutableStateFlow<List<PackageItem>>(
        // Hardcoded data with the new boolean flag for testing
        listOf(
            PackageItem("101", "Inbound Machinery", "Received - Intact", isSentByMe = false),
            PackageItem("102", "Confidential Contract", "Received - Verified", isSentByMe = false),
            PackageItem("103", "Office Supplies", "Received - Processing", isSentByMe = false),
            PackageItem("104", "Outbound Electronics", "In Transit", isSentByMe = true)
        )
    )
    val allPackages: StateFlow<List<PackageItem>> = _allPackages.asStateFlow()

    // --- State for the Filter (0 = Sent, 1 = Received) ---
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    // --- Package Actions ---
    fun addPackage(packageItem: PackageItem) {
        _allPackages.value = _allPackages.value + packageItem
    }
}