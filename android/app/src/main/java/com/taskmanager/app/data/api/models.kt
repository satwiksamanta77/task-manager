package com.taskmanager.app.data.api

// JSON field names match api-contract.md exactly.

data class User(
    val id: String,
    val email: String,
    val name: String,
    val created_at: String,
    val updated_at: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class UserResponse(
    val user: User
)

data class SubtaskDto(
    val id: String,
    val task_id: String,
    val title: String,
    val done: Boolean,
    val position: Int
)

data class TaskDto(
    val id: String,
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val due_at: String?,
    val reminder_at: String?,
    val created_at: String,
    val updated_at: String,
    val subtasks: List<SubtaskDto>?
)

data class NoteDto(
    val id: String,
    val title: String,
    val content: String,
    val color: String,
    val pinned: Boolean,
    val archived: Boolean,
    val created_at: String,
    val updated_at: String
)

data class ScheduleItem(
    val kind: String,
    val id: String,
    val title: String,
    val due_at: String?,
    val reminder_at: String?,
    val priority: String
)

data class TasksResponse(val tasks: List<TaskDto>)
data class TaskResponse(val task: TaskDto)
data class NotesResponse(val notes: List<NoteDto>)
data class NoteResponse(val note: NoteDto)
data class SubtaskResponse(val subtask: SubtaskDto)
data class ScheduleResponse(val items: List<ScheduleItem>)
data class OkResponse(val ok: Boolean)
data class ErrorResponse(val error: String?)

// Request bodies

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val priority: String? = null,
    val due_at: String? = null,
    val reminder_at: String? = null
)

data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val due_at: String? = null,
    val reminder_at: String? = null
)

data class CreateSubtaskRequest(
    val title: String
)

data class UpdateSubtaskRequest(
    val title: String? = null,
    val done: Boolean? = null
)

data class CreateNoteRequest(
    val title: String? = null,
    val content: String? = null,
    val color: String? = null
)

data class UpdateNoteRequest(
    val title: String? = null,
    val content: String? = null,
    val color: String? = null,
    val pinned: Boolean? = null,
    val archived: Boolean? = null
)
