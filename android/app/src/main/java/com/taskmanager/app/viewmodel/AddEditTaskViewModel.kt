package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.LabelDto
import com.taskmanager.app.data.api.ProjectDto
import com.taskmanager.app.data.api.UpdateTaskRequest
import com.taskmanager.app.data.repository.LabelRepository
import com.taskmanager.app.data.repository.ProjectRepository
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
    val projectId: String? = null,
    val recurrence: String = "none",
    val labelIds: Set<String> = emptySet(),
    val projects: List<ProjectDto> = emptyList(),
    val labels: List<LabelDto> = emptyList(),
    val error: String? = null,
    val isNew: Boolean = true
)

class AddEditTaskViewModel(
    private val taskId: String?,
    private val repo: TaskRepository,
    private val projectRepo: ProjectRepository? = null,
    private val labelRepo: LabelRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState(isNew = taskId == null))
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    init {
        loadMeta()
        if (taskId != null) load()
    }

    private fun loadMeta() {
        viewModelScope.launch {
            val projects = try {
                projectRepo?.getProjects().orEmpty()
            } catch (e: Exception) { emptyList() }
            val labels = try {
                labelRepo?.getLabels().orEmpty()
            } catch (e: Exception) { emptyList() }
            _uiState.value = _uiState.value.copy(projects = projects, labels = labels)
        }
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
                    dueAt = task.due_at.orEmpty(),
                    projectId = task.project_id,
                    recurrence = task.recurrence,
                    labelIds = task.labels.map { it.id }.toSet()
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
    fun onProjectChange(v: String?) { _uiState.value = _uiState.value.copy(projectId = v) }
    fun onRecurrenceChange(v: String) { _uiState.value = _uiState.value.copy(recurrence = v) }

    fun toggleLabel(id: String) {
        val current = _uiState.value.labelIds
        _uiState.value = _uiState.value.copy(
            labelIds = if (id in current) current - id else current + id
        )
    }

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
                        dueAt = state.dueAt.trim().ifBlank { null },
                        projectId = state.projectId,
                        recurrence = state.recurrence,
                        labelIds = state.labelIds.toList()
                    )
                } else {
                    repo.updateTask(
                        checkNotNull(taskId) { "taskId is required to update a task" },
                        UpdateTaskRequest(
                            title = state.title.trim(),
                            description = state.description.trim(),
                            priority = state.priority,
                            due_at = state.dueAt.trim().ifBlank { null },
                            project_id = state.projectId,
                            recurrence = state.recurrence,
                            label_ids = state.labelIds.toList()
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
