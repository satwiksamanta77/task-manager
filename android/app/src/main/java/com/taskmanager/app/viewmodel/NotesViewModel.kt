package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.NoteDto
import com.taskmanager.app.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotesUiState(
    val loading: Boolean = false,
    val notes: List<NoteDto> = emptyList(),
    val query: String = "",
    val showArchived: Boolean = false,
    val error: String? = null
)

class NotesViewModel(private val repo: NoteRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState(loading = true))
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val notes = repo.getNotes(_uiState.value.showArchived)
                _uiState.value = _uiState.value.copy(loading = false, notes = notes)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load notes"
                )
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun setShowArchived(show: Boolean) {
        if (_uiState.value.showArchived == show) return
        _uiState.value = _uiState.value.copy(showArchived = show)
        load()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
