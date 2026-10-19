package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.SubtaskDto
import com.taskmanager.app.data.api.TaskDto
import com.taskmanager.app.data.api.UpdateTaskRequest
import com.taskmanager.app.data.repository.ProjectRepository
import com.taskmanager.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val loading: Boolean = false,
    val task: TaskDto? = null,
    val projectName: String? = null,
    val error: String? = null
)

class TaskDetailViewModel(
    private val taskId: String,
    private val repo: TaskRepository,
    private val projectRepo: ProjectRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState(loading = true))
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val task = repo.getTask(taskId)
                val projectName = task.project_id?.let { pid ->
                    try {
                        projectRepo?.getProjects()?.firstOrNull { it.id == pid }?.name
                    } catch (e: Exception) {
                        null
                    }
                }
                _uiState.value = TaskDetailUiState(task = task, projectName = projectName)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load task"
                )
            }
        }
    }

    fun toggleDone() {
        val task = _uiState.value.task ?: return
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

    fun setPriority(priority: String) {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            try {
                repo.updateTask(task.id, UpdateTaskRequest(priority = priority))
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not update task")
            }
        }
    }

    fun addSubtask(title: String) {
        viewModelScope.launch {
            try {
                repo.createSubtask(taskId, title)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not add subtask")
            }
        }
    }

    fun toggleSubtask(subtask: SubtaskDto) {
        viewModelScope.launch {
            try {
                repo.updateSubtask(subtask.id, done = !subtask.done)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not update subtask")
            }
        }
    }

    fun deleteSubtask(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteSubtask(id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not delete subtask")
            }
        }
    }

    fun deleteTask(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.deleteTask(taskId)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not delete task")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
