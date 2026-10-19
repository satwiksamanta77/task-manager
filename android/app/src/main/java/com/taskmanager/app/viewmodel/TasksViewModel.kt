package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.LabelDto
import com.taskmanager.app.data.api.ProjectDto
import com.taskmanager.app.data.api.TaskDto
import com.taskmanager.app.data.api.UpdateTaskRequest
import com.taskmanager.app.data.repository.LabelRepository
import com.taskmanager.app.data.repository.ProjectRepository
import com.taskmanager.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TasksUiState(
    val loading: Boolean = false,
    val tasks: List<TaskDto> = emptyList(),
    val projects: List<ProjectDto> = emptyList(),
    val labels: List<LabelDto> = emptyList(),
    val filter: String? = null,
    val projectFilter: String? = null,
    val labelFilter: String? = null,
    val error: String? = null
)

class TasksViewModel(
    private val repo: TaskRepository,
    private val projectRepo: ProjectRepository? = null,
    private val labelRepo: LabelRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState(loading = true))
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val s = _uiState.value
            _uiState.value = s.copy(loading = true, error = null)
            try {
                val tasks = repo.getTasks(
                    status = s.filter,
                    projectId = s.projectFilter,
                    labelId = s.labelFilter
                )
                val projects = try {
                    projectRepo?.getProjects().orEmpty()
                } catch (e: Exception) { emptyList() }
                val labels = try {
                    labelRepo?.getLabels().orEmpty()
                } catch (e: Exception) { emptyList() }
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    tasks = tasks,
                    projects = projects,
                    labels = labels
                )
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

    fun setProjectFilter(projectId: String?) {
        if (_uiState.value.projectFilter == projectId) return
        _uiState.value = _uiState.value.copy(projectFilter = projectId)
        load()
    }

    fun setLabelFilter(labelId: String?) {
        if (_uiState.value.labelFilter == labelId) return
        _uiState.value = _uiState.value.copy(labelFilter = labelId)
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
