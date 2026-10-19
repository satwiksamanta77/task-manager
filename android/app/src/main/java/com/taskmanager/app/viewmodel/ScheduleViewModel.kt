package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.ScheduleItem
import com.taskmanager.app.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val loading: Boolean = false,
    val items: List<ScheduleItem> = emptyList(),
    val error: String? = null
)

class ScheduleViewModel(private val repo: ScheduleRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState(loading = true))
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val items = repo.getUpcoming(7)
                _uiState.value = ScheduleUiState(items = items)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load schedule"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
