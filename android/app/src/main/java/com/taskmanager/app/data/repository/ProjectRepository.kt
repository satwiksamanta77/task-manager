package com.taskmanager.app.data.repository

import com.taskmanager.app.data.api.ApiService
import com.taskmanager.app.data.api.CreateProjectRequest
import com.taskmanager.app.data.api.ProjectDto
import com.taskmanager.app.data.api.UpdateProjectRequest
import com.taskmanager.app.data.api.unwrap
import com.taskmanager.app.data.local.SessionManager

class ProjectRepository(
    private val api: ApiService,
    session: SessionManager
) : BaseRepository(session) {

    suspend fun getProjects(): List<ProjectDto> =
        unwrap(api.getProjects(authHeader())).projects

    suspend fun createProject(name: String, color: String?): ProjectDto =
        unwrap(api.createProject(authHeader(), CreateProjectRequest(name.trim(), color))).project

    suspend fun updateProject(id: String, name: String?, color: String?): ProjectDto =
        unwrap(api.updateProject(authHeader(), id, UpdateProjectRequest(name?.trim(), color))).project

    suspend fun deleteProject(id: String) {
        unwrap(api.deleteProject(authHeader(), id))
    }
}
