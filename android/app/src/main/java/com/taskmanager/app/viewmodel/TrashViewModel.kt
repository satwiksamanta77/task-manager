package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.NoteDto
import com.taskmanager.app.data.api.TaskDto
import com.taskmanager.app.data.repository.NoteRepository
import com.taskmanager.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrashUiState(
    val loading: Boolean = false,
    val tasks: List<TaskDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val error: String? = null
)

class TrashViewModel(
    private val taskRepo: TaskRepository,
    private val noteRepo: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState(loading = true))
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val tasks = taskRepo.getTasks(trashed = true)
                val notes = noteRepo.getNotes(trashed = true)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    tasks = tasks,
                    notes = notes
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load trash"
                )
            }
        }
    }

    fun restoreTask(id: String) {
        viewModelScope.launch {
            try {
                taskRepo.restoreTask(id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not restore task")
            }
        }
    }

    fun restoreNote(id: String) {
        viewModelScope.launch {
            try {
                noteRepo.restoreNote(id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not restore note")
            }
        }
    }

    fun deleteTaskForever(id: String) {
        viewModelScope.launch {
            try {
                taskRepo.deleteTask(id, permanent = true)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not delete task")
            }
        }
    }

    fun deleteNoteForever(id: String) {
        viewModelScope.launch {
            try {
                noteRepo.deleteNote(id, permanent = true)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not delete note")
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            try {
                _uiState.value.tasks.forEach { taskRepo.deleteTask(it.id, permanent = true) }
                _uiState.value.notes.forEach { noteRepo.deleteNote(it.id, permanent = true) }
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not empty trash")
                load()
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
