package app.cryptoseal.tabs.activity

import app.cryptoseal.data.api.ApiService
import app.cryptoseal.data.model.Activity
import io.mockk.coEvery
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
class ActivityViewModelTest {

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
    fun `initialization fetches activities successfully`() = runTest {
        // Given
        val mockActivities = listOf(
            Activity(
                id = 1,
                actorId = 1,
                userId = 2,
                type = "ORDER_CREATED",
                summary = "Order 1 created",
                createdAt = "2023-01-01T00:00:00Z"
            ),
            Activity(
                id = 2,
                actorId = 2,
                userId = 1,
                type = "SCAN_CREATED",
                summary = "Order 1 scanned",
                createdAt = "2023-01-01T01:00:00Z"
            )
        )
        coEvery { ApiService.getActivities() } returns Result.success(mockActivities)

        // When
        val viewModel = ActivityViewModel()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
        assertEquals(2, viewModel.activities.value.size)
        assertEquals("ORDER_CREATED", viewModel.activities.value[0].type)
    }

    @Test
    fun `refreshActivities failure updates error state`() = runTest {
        // Given
        coEvery { ApiService.getActivities() } returns Result.failure(Exception("Network Error"))

        // When
        val viewModel = ActivityViewModel()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isLoading.value)
        assertEquals("Network Error", viewModel.error.value)
        assertTrue(viewModel.activities.value.isEmpty())
    }

    @Test
    fun `refreshActivities clears previous error on success`() = runTest {
        // Given
        coEvery { ApiService.getActivities() } returns Result.failure(Exception("Initial Error"))
        val viewModel = ActivityViewModel()
        advanceUntilIdle()
        assertEquals("Initial Error", viewModel.error.value)

        val mockActivities = listOf(
            Activity(
                id = 1,
                actorId = 1,
                userId = 2,
                type = "TEST",
                summary = "Test",
                createdAt = "2023-01-01T00:00:00Z"
            )
        )
        coEvery { ApiService.getActivities() } returns Result.success(mockActivities)

        // When
        viewModel.refreshActivities()
        advanceUntilIdle()

        // Then
        assertNull(viewModel.error.value)
        assertEquals(1, viewModel.activities.value.size)
    }
}
