package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.UpdateTaskRequest
import com.taskmanager.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEditTaskUiState(
    val loading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val priority: String = "medium",
    val dueAt: String = "",
    val error: String? = null,
    val isNew: Boolean = true
)

class AddEditTaskViewModel(
    private val taskId: String?,
    private val repo: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState(isNew = taskId == null))
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    init {
        if (taskId != null) load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val id = checkNotNull(taskId) { "taskId is required to load a task" }
                val task = repo.getTask(id)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    title = task.title,
                    description = task.description.orEmpty(),
                    priority = task.priority,
                    dueAt = task.due_at.orEmpty()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load task"
                )
            }
        }
    }

    fun onTitleChange(v: String) { _uiState.value = _uiState.value.copy(title = v) }
    fun onDescriptionChange(v: String) { _uiState.value = _uiState.value.copy(description = v) }
    fun onPriorityChange(v: String) { _uiState.value = _uiState.value.copy(priority = v) }
    fun onDueAtChange(v: String) { _uiState.value = _uiState.value.copy(dueAt = v) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(error = "Title is required")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            try {
                if (state.isNew) {
                    repo.createTask(
                        title = state.title.trim(),
                        description = state.description.trim().ifBlank { null },
                        priority = state.priority,
                        dueAt = state.dueAt.trim().ifBlank { null }
                    )
                } else {
                    repo.updateTask(
                        checkNotNull(taskId) { "taskId is required to update a task" },
                        UpdateTaskRequest(
                            title = state.title.trim(),
                            description = state.description.trim(),
                            priority = state.priority,
                            due_at = state.dueAt.trim().ifBlank { null }
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(loading = false)
                onSaved()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not save task"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
