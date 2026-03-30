package app.cryptoseal.tabs.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cryptoseal.data.api.ApiService
import app.cryptoseal.data.model.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the "Activity" tab, responsible for managing its data and state.
 *
 * This class follows the Unidirectional Data Flow (UDF) pattern by exposing 
 * state via [StateFlow] and providing methods to trigger state changes. It acts 
 * as the intermediary between the [ApiService] and the [ActivityTab] UI.
 */
class ActivityViewModel : ViewModel() {

    // Internal mutable state for the list of activity items. 
    // Initialized as an empty list.
    private val _activities = MutableStateFlow<List<Activity>>(emptyList())

    /**
     * A read-only [StateFlow] containing the list of recent [Activity] objects.
     * The UI (ActivityTab) observes this flow to automatically update when new data arrives.
     */
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    // Internal mutable state to track if a network request is currently active.
    private val _isLoading = MutableStateFlow(false)

    /**
     * A read-only [StateFlow] that indicates the current loading status.
     * Used by the UI to show or hide progress indicators (like PullToRefresh).
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Internal state to hold any error messages encountered during API calls.
    private val _error = MutableStateFlow<String?>(null)

    /**
     * A read-only [StateFlow] representing the last error message, or null if no error exists.
     */
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Initialization block. 
     * Automatically triggers a data refresh when the ViewModel is first created.
     */
    init {
        refreshActivities()
    }

    /**
     * Fetches the latest system activities from the backend.
     * 
     * This function launches a coroutine in the [viewModelScope], ensuring that 
     * the request is cancelled if the user navigates away and the ViewModel is cleared.
     */
    fun refreshActivities() {
        viewModelScope.launch {
            // Step 1: Set UI to loading state and clear previous errors.
            _isLoading.value = true
            _error.value = null

            Log.d("ActivityViewModel", "Refreshing activities...")

            // Step 2: Perform the network request via the ApiService.
            ApiService.getActivities()
                .onSuccess { activityList ->
                    // On Success: Update the activities list. 
                    // The server typically returns these in reverse chronological order.
                    Log.d("ActivityViewModel", "Fetched ${activityList.size} activities")
                    _activities.value = activityList
                }
                .onFailure { exception ->
                    // On Failure: Log the error and update the error state for the UI.
                    Log.e("ActivityViewModel", "Error fetching activities", exception)
                    _error.value = exception.message ?: "Failed to fetch activities"
                }

            // Step 3: Clear the loading state regardless of success or failure.
            _isLoading.value = false
        }
    }
}
