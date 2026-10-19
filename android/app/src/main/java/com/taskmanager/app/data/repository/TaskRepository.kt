package com.taskmanager.app.data.repository

import com.taskmanager.app.data.api.ApiService
import com.taskmanager.app.data.api.CreateSubtaskRequest
import com.taskmanager.app.data.api.CreateTaskRequest
import com.taskmanager.app.data.api.SubtaskDto
import com.taskmanager.app.data.api.TaskDto
import com.taskmanager.app.data.api.UpdateSubtaskRequest
import com.taskmanager.app.data.api.UpdateTaskRequest
import com.taskmanager.app.data.api.unwrap
import com.taskmanager.app.data.local.SessionManager

class TaskRepository(
    private val api: ApiService,
    session: SessionManager
) : BaseRepository(session) {

    suspend fun getTasks(status: String? = null): List<TaskDto> =
        unwrap(api.getTasks(authHeader(), status)).tasks

    suspend fun createTask(
        title: String,
        description: String?,
        priority: String?,
        dueAt: String?
    ): TaskDto = unwrap(
        api.createTask(
            authHeader(),
            CreateTaskRequest(
                title = title,
                description = description?.ifBlank { null },
                priority = priority,
                due_at = dueAt?.ifBlank { null }
            )
        )
    ).task

    suspend fun getTask(id: String): TaskDto =
        unwrap(api.getTask(authHeader(), id)).task

    suspend fun updateTask(id: String, body: UpdateTaskRequest): TaskDto =
        unwrap(api.updateTask(authHeader(), id, body)).task

    suspend fun deleteTask(id: String) {
        unwrap(api.deleteTask(authHeader(), id))
    }

    suspend fun createSubtask(taskId: String, title: String): SubtaskDto =
        unwrap(api.createSubtask(authHeader(), taskId, CreateSubtaskRequest(title))).subtask

    suspend fun updateSubtask(id: String, title: String? = null, done: Boolean? = null): SubtaskDto =
        unwrap(api.updateSubtask(authHeader(), id, UpdateSubtaskRequest(title, done))).subtask

    suspend fun deleteSubtask(id: String) {
        unwrap(api.deleteSubtask(authHeader(), id))
    }
}
