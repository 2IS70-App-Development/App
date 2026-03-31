package app.cryptoseal.tabs.scanner

import android.app.Application
import android.graphics.Bitmap
import android.location.Location
import android.util.Base64
import app.cryptoseal.data.api.ApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import io.mockk.any
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val fusedLocationClient = mockk<FusedLocationProviderClient>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(any<Application>()) } returns fusedLocationClient

        mockkObject(ApiService)
        mockkStatic(Tasks::class)
        mockkStatic(Base64::class)
        mockkStatic(Bitmap::class)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `onOrderIdScanned sets scannedOrderId when null`() {
        val viewModel = ScannerViewModel(application)
        viewModel.onOrderIdScanned(123)
        assertEquals(123, viewModel.scannedOrderId)
    }

    @Test
    fun `onOrderIdScanned does not overwrite existing orderId`() {
        val viewModel = ScannerViewModel(application)
        viewModel.onOrderIdScanned(123)
        viewModel.onOrderIdScanned(456)
        assertEquals(123, viewModel.scannedOrderId)
    }

    @Test
    fun `resetScanner clears all state`() {
        val viewModel = ScannerViewModel(application)
        viewModel.onOrderIdScanned(123)
        viewModel.condition = "Damaged"
        viewModel.comment = "Broken"
        viewModel.submitError = "Some error"
        viewModel.submitSuccess = true

        viewModel.resetScanner()

        assertNull(viewModel.scannedOrderId)
        assertEquals("Good", viewModel.condition)
        assertEquals("", viewModel.comment)
        assertNull(viewModel.photoBitmap)
        assertNull(viewModel.submitError)
        assertFalse(viewModel.submitSuccess)
    }

    @Test
    fun `submitScan without photo sets error`() = runTest {
        val viewModel = ScannerViewModel(application)
        viewModel.onOrderIdScanned(123)

        viewModel.submitScan()
        advanceUntilIdle()

        assertEquals("Please take a photo of the package.", viewModel.submitError)
    }

    @Test
    fun `submitScan success updates state`() = runTest {
        // Given
        val viewModel = ScannerViewModel(application)
        viewModel.onOrderIdScanned(123)
        val mockBitmap = mockk<Bitmap>()
        viewModel.photoBitmap = mockBitmap

        // Mock Location
        val mockLocation = mockk<Location>()
        every { mockLocation.latitude } returns 1.0
        every { mockLocation.longitude } returns 2.0
        val mockTask = mockk<Task<Location>>()
        every { fusedLocationClient.lastLocation } returns mockTask
        every { Tasks.await(mockTask, any<Long>(), any()) } returns mockLocation

        // Mock Bitmap compression
        every { mockBitmap.compress(any(), any(), any<OutputStream>()) } returns true

        // Mock Base64
        every { Base64.encodeToString(any(), any()) } returns "base64photo"

        // Mock API
        coEvery {
            ApiService.createOrderScan(
                123,
                "base64photo",
                "Good",
                2.0f,
                1.0f,
                ""
            )
        } returns Result.success(Unit)

        // When
        viewModel.submitScan()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.submitSuccess)
        assertNull(viewModel.scannedOrderId)
        assertFalse(viewModel.isSubmitting)
    }

    @Test
    fun `submitScan failure updates error state`() = runTest {
        // Given
        val viewModel = ScannerViewModel(application)
        viewModel.onOrderIdScanned(123)
        val mockBitmap = mockk<Bitmap>()
        viewModel.photoBitmap = mockBitmap

        // Mock Location failure
        val mockTask = mockk<Task<Location>>()
        every { fusedLocationClient.lastLocation } returns mockTask
        every { Tasks.await(mockTask, any<Long>(), any()) } returns null

        // Mock Bitmap compression
        every { mockBitmap.compress(any(), any(), any()) } returns true

        // Mock Base64
        every { Base64.encodeToString(any(), any()) } returns "base64photo"

        // Mock API Failure
        coEvery {
            ApiService.createOrderScan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Result.failure(Exception("API Error"))

        // When
        viewModel.submitScan()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.submitSuccess)
        assertEquals("API Error", viewModel.submitError)
        assertFalse(viewModel.isSubmitting)
    }
}
