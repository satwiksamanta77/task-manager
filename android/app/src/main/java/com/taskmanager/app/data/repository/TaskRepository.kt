package com.taskmanager.app.data.repository

import com.taskmanager.app.data.api.ApiService
import com.taskmanager.app.data.api.CreateSubtaskRequest
import com.taskmanager.app.data.api.CreateTaskRequest
import com.taskmanager.app.data.api.SubtaskDto
import com.taskmanager.app.data.api.TaskDto
import com.taskmanager.app.data.api.UpdateSubtaskRequest
import com.taskmanager.app.data.api.UpdateTaskRequest
import com.taskmanager.app.data.api.UpdateTaskResponse
import com.taskmanager.app.data.api.unwrap
import com.taskmanager.app.data.local.SessionManager

class TaskRepository(
    private val api: ApiService,
    session: SessionManager
) : BaseRepository(session) {

    suspend fun getTasks(
        status: String? = null,
        projectId: String? = null,
        labelId: String? = null,
        trashed: Boolean = false
    ): List<TaskDto> =
        unwrap(api.getTasks(authHeader(), status, projectId, labelId, trashed.takeIf { it })).tasks

    suspend fun createTask(
        title: String,
        description: String?,
        priority: String?,
        dueAt: String?,
        projectId: String? = null,
        recurrence: String = "none",
        labelIds: List<String> = emptyList()
    ): TaskDto = unwrap(
        api.createTask(
            authHeader(),
            CreateTaskRequest(
                title = title,
                description = description?.ifBlank { null },
                priority = priority,
                due_at = dueAt?.ifBlank { null },
                project_id = projectId,
                recurrence = recurrence.takeIf { it != "none" },
                label_ids = labelIds.takeIf { it.isNotEmpty() }
            )
        )
    ).task

    suspend fun getTask(id: String): TaskDto =
        unwrap(api.getTask(authHeader(), id)).task

    suspend fun updateTask(id: String, body: UpdateTaskRequest): UpdateTaskResponse =
        unwrap(api.updateTask(authHeader(), id, body))

    suspend fun deleteTask(id: String, permanent: Boolean = false) {
        unwrap(api.deleteTask(authHeader(), id, permanent.takeIf { it }))
    }

    suspend fun restoreTask(id: String): TaskDto =
        unwrap(api.restoreTask(authHeader(), id)).task

    suspend fun createSubtask(taskId: String, title: String): SubtaskDto =
        unwrap(api.createSubtask(authHeader(), taskId, CreateSubtaskRequest(title))).subtask

    suspend fun updateSubtask(id: String, title: String? = null, done: Boolean? = null): SubtaskDto =
        unwrap(api.updateSubtask(authHeader(), id, UpdateSubtaskRequest(title, done))).subtask

    suspend fun deleteSubtask(id: String) {
        unwrap(api.deleteSubtask(authHeader(), id))
    }
}
