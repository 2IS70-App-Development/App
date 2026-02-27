package app.cryptoseal.feature.packages

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class PackagesViewModel : ViewModel() {
    // This holds the state of our list. It starts empty.
    private val _sendingList = MutableStateFlow<List<PackageItem>>(emptyList())
    val sendingList: StateFlow<List<PackageItem>> = _sendingList.asStateFlow()

    // This function handles the logic when the "Generate" button is clicked
    fun createAndAddPackage(name: String, routingInfo: String, manifestData: String): String {
        // Generate a unique Order ID for the QR code
        val orderId = UUID.randomUUID().toString()

        // Create the new package item
        val newPackage = PackageItem(
            id = orderId,
            name = name,
            status = "Ready for transit"
        )

        // Add it to our state list so the UI updates automatically
        _sendingList.value = _sendingList.value + newPackage

        // Return the Order ID so the UI can generate the QR code graphic
        return orderId
    }
}