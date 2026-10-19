package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.ProjectDto
import com.taskmanager.app.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val loading: Boolean = false,
    val projects: List<ProjectDto> = emptyList(),
    val error: String? = null
)

class ProjectsViewModel(private val repo: ProjectRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState(loading = true))
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    projects = repo.getProjects()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load projects"
                )
            }
        }
    }

    fun create(name: String, color: String?) {
        viewModelScope.launch {
            try {
                repo.createProject(name, color)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not create project")
            }
        }
    }

    fun update(id: String, name: String, color: String?) {
        viewModelScope.launch {
            try {
                repo.updateProject(id, name, color)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not update project")
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteProject(id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not delete project")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
