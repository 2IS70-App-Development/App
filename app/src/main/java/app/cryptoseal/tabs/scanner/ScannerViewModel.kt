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

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    var scannedOrderId by mutableStateOf<Int?>(null)
        private set

    var condition by mutableStateOf("Good")
    var comment by mutableStateOf("")
    var photoBitmap by mutableStateOf<Bitmap?>(null)

    var isSubmitting by mutableStateOf(false)
    var submitError by mutableStateOf<String?>(null)
    var submitSuccess by mutableStateOf(false)

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    fun onOrderIdScanned(orderId: Int) {
        if (scannedOrderId == null && !isSubmitting && !submitSuccess) {
            scannedOrderId = orderId
        }
    }

    fun resetScanner() {
        scannedOrderId = null
        condition = "Good"
        comment = ""
        photoBitmap = null
        submitError = null
        submitSuccess = false
    }

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
                    val location: Location? =
                        Tasks.await(fusedLocationClient.lastLocation, 5, TimeUnit.SECONDS)
                    if (location != null) {
                        lat = location.latitude.toFloat()
                        lon = location.longitude.toFloat()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Process Bitmap to Base64
                val outputStream = ByteArrayOutputStream()
                photoBitmap?.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64Photo = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                // 3. API Call
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
