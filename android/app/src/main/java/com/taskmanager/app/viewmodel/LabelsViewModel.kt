package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.LabelDto
import com.taskmanager.app.data.repository.LabelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LabelsUiState(
    val loading: Boolean = false,
    val labels: List<LabelDto> = emptyList(),
    val error: String? = null
)

class LabelsViewModel(private val repo: LabelRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LabelsUiState(loading = true))
    val uiState: StateFlow<LabelsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    labels = repo.getLabels()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load labels"
                )
            }
        }
    }

    fun create(name: String, color: String?) {
        viewModelScope.launch {
            try {
                repo.createLabel(name, color)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not create label")
            }
        }
    }

    fun update(id: String, name: String, color: String?) {
        viewModelScope.launch {
            try {
                repo.updateLabel(id, name, color)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not update label")
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteLabel(id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not delete label")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
