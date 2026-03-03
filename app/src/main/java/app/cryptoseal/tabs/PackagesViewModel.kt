package app.cryptoseal.tabs

import androidx.lifecycle.ViewModel
import app.cryptoseal.feature.packages.PackageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class PackagesViewModel : ViewModel() {

    // --- State for the Lists ---
    private val _sendingList = MutableStateFlow<List<PackageItem>>(emptyList())
    val sendingList: StateFlow<List<PackageItem>> = _sendingList.asStateFlow()

    private val _receivingList = MutableStateFlow<List<PackageItem>>(
        // Moved the hardcoded data here so it persists
        listOf(
            PackageItem("101", "Inbound Machinery", "Received - Intact"),
            PackageItem("102", "Confidential Contract", "Received - Verified"),
            PackageItem("103", "Office Supplies", "Received - Processing")
        )
    )
    val receivingList: StateFlow<List<PackageItem>> = _receivingList.asStateFlow()

    // --- State for the Filter (0 = Sent, 1 = Received) ---
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun createAndAddPackage(name: String, routingInfo: String, manifestData: String): String {
        val orderId = UUID.randomUUID().toString()
        val newPackage = PackageItem(orderId, name, "Ready for transit")
        _sendingList.value = _sendingList.value + newPackage
        return orderId
    }
}