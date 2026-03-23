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

class ActivityViewModel : ViewModel() {

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refreshActivities()
    }

    fun refreshActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("ActivityViewModel", "Refreshing activities...")
            ApiService.getActivities()
                .onSuccess { activityList ->
                    Log.d("ActivityViewModel", "Fetched ${activityList.size} activities")
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
