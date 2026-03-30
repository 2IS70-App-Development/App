package app.cryptoseal.tabs

import app.cryptoseal.data.api.ApiService
import app.cryptoseal.data.model.Order
import app.cryptoseal.data.model.User
import io.mockk.coEvery
import io.mockk.every
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
class PackagesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(ApiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `refreshPackages success updates allPackages`() = runTest {
        // Given
        val mockUser = User(id = 1, email = "test@example.com", createdAt = "2023-01-01T00:00:00Z")
        val mockOrders = listOf(
            Order(
                id = 101,
                name = "Package 1",
                status = "PENDING",
                senderId = 1,
                receiverId = 2,
                meta = "",
                comment = "",
                createdAt = "2023-01-01T00:00:00Z"
            ),
            Order(
                id = 102,
                name = "Package 2",
                status = "DELIVERED",
                senderId = 3,
                receiverId = 1,
                meta = "",
                comment = "",
                createdAt = "2023-01-01T00:00:00Z"
            )
        )

        every { ApiService.currentUser } returns mockUser
        coEvery { ApiService.getOrders() } returns Result.success(mockOrders)
        coEvery { ApiService.getUsers() } returns Result.success(listOf(mockUser))

        // When
        val viewModel = PackagesViewModel()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
        assertEquals(2, viewModel.allPackages.value.size)

        // Verify mapping logic (directionality)
        assertTrue(viewModel.allPackages.value[0].isSentByMe) // order 101 senderId is 1 (current user)
        assertFalse(viewModel.allPackages.value[1].isSentByMe) // order 102 senderId is 3
    }

    @Test
    fun `refreshPackages failure updates error state`() = runTest {
        // Given
        every { ApiService.currentUser } returns null
        coEvery { ApiService.getOrders() } returns Result.failure(Exception("Network Error"))
        coEvery { ApiService.getUsers() } returns Result.success(emptyList())

        // When
        val viewModel = PackagesViewModel()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isLoading.value)
        assertEquals("Network Error", viewModel.error.value)
        assertTrue(viewModel.allPackages.value.isEmpty())
    }

    @Test
    fun `setTab updates selectedTab state`() = runTest {
        // Given
        coEvery { ApiService.getOrders() } returns Result.success(emptyList())
        coEvery { ApiService.getUsers() } returns Result.success(emptyList())

        val viewModel = PackagesViewModel()
        advanceUntilIdle()

        // When
        viewModel.setTab(1)

        // Then
        assertEquals(1, viewModel.selectedTab.value)
    }
}
