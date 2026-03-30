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
 * ViewModel for the "Activity" tab.
 * Responsible for fetching and providing a list of recent system activities/notifications.
 */
class ActivityViewModel : ViewModel() {

    // Internal state for the list of activity items.
    private val _activities = MutableStateFlow<List<Activity>>(emptyList())

    /**
     * Publicly exposed state flow for observing the recent activities.
     */
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    // Internal loading state.
    private val _isLoading = MutableStateFlow(false)

    /**
     * Publicly exposed state flow indicating if an activity fetch is in progress.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Internal error message state.
    private val _error = MutableStateFlow<String?>(null)

    /**
     * Publicly exposed state flow for error messages.
     */
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Automatically triggers a refresh of activities when the ViewModel is initialized.
     */
    init {
        refreshActivities()
    }

    /**
     * Fetches the latest activities from the backend [ApiService].
     * Updates [activities], [isLoading], and [error] states accordingly.
     */
    fun refreshActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("ActivityViewModel", "Refreshing activities...")
            ApiService.getActivities()
                .onSuccess { activityList ->
                    Log.d("ActivityViewModel", "Fetched ${activityList.size} activities")
                    // The list is typically returned in reverse chronological order from the server.
                    _activities.value = activityList
                }
                .onFailure {
                    Log.e("ActivityViewModel", "Error fetching activities", it)
                    _error.value = it.message ?: "Failed to fetch activities"
                }
            _isLoading.value = false
        }
    }
}
