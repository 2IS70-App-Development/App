package app.cryptoseal.tabs.scanner

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.location.Location
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cryptoseal.data.api.ApiService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * ViewModel for the "Scanner" tab.
 * Manages the state of the QR scanner, handles scanned order IDs, captures package photos,
 * retrieves GPS location, and submits scan records to the backend.
 *
 * @param application The application context, required for location services.
 */
class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * The ID of the package currently being scanned/processed.
     * Set when a valid QR code is detected.
     */
    var scannedOrderId by mutableStateOf<Int?>(null)
        private set

    /**
     * User-selected condition of the package (e.g., "Good", "Missing", "Damaged").
     */
    var condition by mutableStateOf("Good")

    /**
     * Additional notes provided by the user about the scan.
     */
    var comment by mutableStateOf("")

    /**
     * The photo captured by the user during the scan process.
     */
    var photoBitmap by mutableStateOf<Bitmap?>(null)

    /**
     * Indicates if a scan submission is currently in progress.
     */
    var isSubmitting by mutableStateOf(false)

    /**
     * Holds any error message resulting from a failed submission.
     */
    var submitError by mutableStateOf<String?>(null)

    /**
     * True if the last submission was successful, used to trigger success UI.
     */
    var submitSuccess by mutableStateOf(false)

    // Client for interacting with Google Play Services location APIs.
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    /**
     * Called when the camera detects a QR code.
     * Starts the submission flow for the given order ID if nothing else is in progress.
     */
    fun onOrderIdScanned(orderId: Int) {
        if (scannedOrderId == null && !isSubmitting && !submitSuccess) {
            scannedOrderId = orderId
        }
    }

    /**
     * Resets the ViewModel to its initial state, clearing all scanned data and errors.
     * Useful for starting a new scan or dismissing the current one.
     */
    fun resetScanner() {
        scannedOrderId = null
        condition = "Good"
        comment = ""
        photoBitmap = null
        submitError = null
        submitSuccess = false
    }

    /**
     * Collects all current scan data (photo, location, condition) and sends it to the server.
     * Requires location permissions to be granted at the call site.
     */
    @SuppressLint("MissingPermission")
    fun submitScan() {
        val orderId = scannedOrderId ?: return

        if (photoBitmap == null) {
            submitError = "Please take a photo of the package."
            return
        }

        isSubmitting = true
        submitError = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get Location (with a timeout/fallback)
                var lat = 0.0f
                var lon = 0.0f
                try {
                    // Tasks.await blocks the current thread until the location is retrieved or times out.
                    val location: Location? =
                        Tasks.await(fusedLocationClient.lastLocation, 5, TimeUnit.SECONDS)
                    if (location != null) {
                        lat = location.latitude.toFloat()
                        lon = location.longitude.toFloat()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Process Bitmap to Base64 for network transmission.
                val outputStream = ByteArrayOutputStream()
                photoBitmap?.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64Photo = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                // 3. API Call to register the scan.
                val result = ApiService.createOrderScan(
                    orderId = orderId,
                    photoBase64 = base64Photo,
                    condition = condition,
                    longitude = lon,
                    latitude = lat,
                    comment = comment
                )

                launch(Dispatchers.Main) {
                    if (result.isSuccess) {
                        submitSuccess = true
                        scannedOrderId = null // Close sheet on success
                    } else {
                        submitError = result.exceptionOrNull()?.message ?: "Submission failed"
                    }
                    isSubmitting = false
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    submitError = e.message ?: "An unexpected error occurred"
                    isSubmitting = false
                }
            }
        }
    }
}
