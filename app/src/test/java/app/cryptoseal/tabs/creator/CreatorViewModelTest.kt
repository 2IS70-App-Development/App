package app.cryptoseal.tabs.creator

import android.graphics.Bitmap
import app.cryptoseal.data.api.ApiService
import app.cryptoseal.data.model.Order
import app.cryptoseal.data.model.User
import app.cryptoseal.util.QRUtils
import io.mockk.any
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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

@OptIn(ExperimentalCoroutinesApi::class)
class CreatorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(ApiService)
        mockkObject(QRUtils)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadAllUsers success filters current user`() = runTest {
        // Given
        val currentUser = User(id = 1, email = "me@example.com", createdAt = "")
        val otherUser = User(id = 2, email = "other@example.com", createdAt = "")

        every { ApiService.currentUser } returns currentUser
        coEvery { ApiService.getUsers() } returns Result.success(listOf(currentUser, otherUser))

        val viewModel = CreatorViewModel()

        // When
        viewModel.loadAllUsers()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isLoadingUsers.value)
        assertEquals(1, viewModel.users.value.size)
        assertEquals(2, viewModel.users.value[0].id)
        assertEquals("other@example.com", viewModel.users.value[0].email)
    }

    @Test
    fun `loadAllUsers failure sets empty list`() = runTest {
        // Given
        coEvery { ApiService.getUsers() } returns Result.failure(Exception("Error"))

        val viewModel = CreatorViewModel()

        // When
        viewModel.loadAllUsers()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.users.value.isEmpty())
    }

    @Test
    fun `createOrder success updates result with bitmap`() = runTest {
        // Given
        val mockOrder = Order(
            id = 123,
            senderId = 1,
            receiverId = 2,
            name = "Test",
            status = "SENT",
            meta = "",
            comment = "",
            createdAt = ""
        )
        val mockBitmap = mockk<Bitmap>()

        coEvery {
            ApiService.createOrder(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Result.success(mockOrder)
        every { QRUtils.generateQrBitmap("123", any()) } returns mockBitmap

        val viewModel = CreatorViewModel()

        // When
        viewModel.createOrder(2, "Test", "", "", null)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isCreatingOrder.value)
        val result = viewModel.createOrderResult.value
        assertTrue(result is CreateOrderResult.Success)
        assertEquals(mockOrder, (result as CreateOrderResult.Success).order)
        assertEquals(mockBitmap, result.qrBitmap)
    }

    @Test
    fun `createOrder failure updates result with error`() = runTest {
        // Given
        coEvery {
            ApiService.createOrder(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Result.failure(Exception("Network Error"))

        val viewModel = CreatorViewModel()

        // When
        viewModel.createOrder(2, "Test", "", "", null)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isCreatingOrder.value)
        val result = viewModel.createOrderResult.value
        assertTrue(result is CreateOrderResult.Error)
        assertEquals("Network Error", (result as CreateOrderResult.Error).message)
    }

    @Test
    fun `clearCreateResult resets state`() = runTest {
        // Given
        val viewModel = CreatorViewModel()
        coEvery {
            ApiService.createOrder(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Result.failure(Exception("Error"))
        viewModel.createOrder(2, "Test", "", "", null)
        advanceUntilIdle()

        // When
        viewModel.clearCreateResult()

        // Then
        assertNull(viewModel.createOrderResult.value)
    }
}
