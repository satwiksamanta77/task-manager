package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.TaskDto
import com.taskmanager.app.data.api.UpdateTaskRequest
import com.taskmanager.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TasksUiState(
    val loading: Boolean = false,
    val tasks: List<TaskDto> = emptyList(),
    val filter: String? = null,
    val error: String? = null
)

class TasksViewModel(private val repo: TaskRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState(loading = true))
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val tasks = repo.getTasks(_uiState.value.filter)
                _uiState.value = _uiState.value.copy(loading = false, tasks = tasks)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load tasks"
                )
            }
        }
    }

    fun setFilter(filter: String?) {
        if (_uiState.value.filter == filter) return
        _uiState.value = _uiState.value.copy(filter = filter)
        load()
    }

    fun toggleDone(task: TaskDto) {
        viewModelScope.launch {
            try {
                val next = if (task.status == "done") "todo" else "done"
                repo.updateTask(task.id, UpdateTaskRequest(status = next))
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not update task")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
