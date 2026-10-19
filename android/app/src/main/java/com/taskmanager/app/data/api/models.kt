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

data class LabelDto(
    val id: String,
    val name: String,
    val color: String?,
    val created_at: String
)

data class ProjectDto(
    val id: String,
    val name: String,
    val color: String?,
    val position: Int,
    val created_at: String
)

data class ChecklistItemDto(
    val id: String,
    val note_id: String,
    val text: String,
    val checked: Boolean,
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
    val project_id: String?,
    val recurrence: String,
    val deleted_at: String?,
    val created_at: String,
    val updated_at: String,
    val labels: List<LabelDto>,
    val subtasks: List<SubtaskDto>?
)

data class NoteDto(
    val id: String,
    val title: String,
    val content: String,
    val color: String,
    val pinned: Boolean,
    val archived: Boolean,
    val deleted_at: String?,
    val created_at: String,
    val updated_at: String,
    val labels: List<LabelDto>
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
/** PUT /api/tasks/:id — completing a recurring task also returns the spawned next_task. */
data class UpdateTaskResponse(val task: TaskDto, val next_task: TaskDto? = null)
data class NotesResponse(val notes: List<NoteDto>)
data class NoteResponse(val note: NoteDto)
data class SubtaskResponse(val subtask: SubtaskDto)
data class ScheduleResponse(val items: List<ScheduleItem>)
data class OkResponse(val ok: Boolean)
data class ErrorResponse(val error: String?)

data class ProjectsResponse(val projects: List<ProjectDto>)
data class ProjectResponse(val project: ProjectDto)
data class LabelsResponse(val labels: List<LabelDto>)
data class LabelResponse(val label: LabelDto)
data class ChecklistItemsResponse(val items: List<ChecklistItemDto>)
data class ChecklistItemResponse(val item: ChecklistItemDto)
data class SearchResponse(val tasks: List<TaskDto>, val notes: List<NoteDto>)

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
    val reminder_at: String? = null,
    val project_id: String? = null,
    val recurrence: String? = null,
    val label_ids: List<String>? = null
)

data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val due_at: String? = null,
    val reminder_at: String? = null,
    val project_id: String? = null,
    val recurrence: String? = null,
    val label_ids: List<String>? = null
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
    val color: String? = null,
    val label_ids: List<String>? = null
)

data class UpdateNoteRequest(
    val title: String? = null,
    val content: String? = null,
    val color: String? = null,
    val pinned: Boolean? = null,
    val archived: Boolean? = null,
    val label_ids: List<String>? = null
)

data class CreateProjectRequest(
    val name: String,
    val color: String? = null
)

data class UpdateProjectRequest(
    val name: String? = null,
    val color: String? = null,
    val position: Int? = null
)

data class CreateLabelRequest(
    val name: String,
    val color: String? = null
)

data class UpdateLabelRequest(
    val name: String? = null,
    val color: String? = null
)

data class CreateChecklistItemRequest(
    val text: String
)

data class UpdateChecklistItemRequest(
    val text: String? = null,
    val checked: Boolean? = null,
    val position: Int? = null
)
