package com.taskmanager.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") auth: String): Response<OkResponse>

    @GET("api/auth/me")
    suspend fun me(@Header("Authorization") auth: String): Response<UserResponse>

    @GET("api/tasks")
    suspend fun getTasks(
        @Header("Authorization") auth: String,
        @Query("status") status: String? = null,
        @Query("project_id") projectId: String? = null,
        @Query("label_id") labelId: String? = null,
        @Query("trashed") trashed: Boolean? = null
    ): Response<TasksResponse>

    @POST("api/tasks")
    suspend fun createTask(
        @Header("Authorization") auth: String,
        @Body body: CreateTaskRequest
    ): Response<TaskResponse>

    @GET("api/tasks/{id}")
    suspend fun getTask(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<TaskResponse>

    @PUT("api/tasks/{id}")
    suspend fun updateTask(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: UpdateTaskRequest
    ): Response<UpdateTaskResponse>

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Query("permanent") permanent: Boolean? = null
    ): Response<OkResponse>

    @POST("api/tasks/{id}/restore")
    suspend fun restoreTask(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<TaskResponse>

    @POST("api/tasks/{id}/subtasks")
    suspend fun createSubtask(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: CreateSubtaskRequest
    ): Response<SubtaskResponse>

    @PUT("api/subtasks/{id}")
    suspend fun updateSubtask(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: UpdateSubtaskRequest
    ): Response<SubtaskResponse>

    @DELETE("api/subtasks/{id}")
    suspend fun deleteSubtask(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<OkResponse>

    @GET("api/notes")
    suspend fun getNotes(
        @Header("Authorization") auth: String,
        @Query("archived") archived: Boolean? = null,
        @Query("label_id") labelId: String? = null,
        @Query("trashed") trashed: Boolean? = null
    ): Response<NotesResponse>

    @POST("api/notes")
    suspend fun createNote(
        @Header("Authorization") auth: String,
        @Body body: CreateNoteRequest
    ): Response<NoteResponse>

    @GET("api/notes/{id}")
    suspend fun getNote(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<NoteResponse>

    @PUT("api/notes/{id}")
    suspend fun updateNote(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: UpdateNoteRequest
    ): Response<NoteResponse>

    @DELETE("api/notes/{id}")
    suspend fun deleteNote(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Query("permanent") permanent: Boolean? = null
    ): Response<OkResponse>

    @POST("api/notes/{id}/restore")
    suspend fun restoreNote(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<NoteResponse>

    @GET("api/notes/{id}/items")
    suspend fun getChecklistItems(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<ChecklistItemsResponse>

    @POST("api/notes/{id}/items")
    suspend fun createChecklistItem(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: CreateChecklistItemRequest
    ): Response<ChecklistItemResponse>

    @PUT("api/notes/{id}/items/{itemId}")
    suspend fun updateChecklistItem(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Path("itemId") itemId: String,
        @Body body: UpdateChecklistItemRequest
    ): Response<ChecklistItemResponse>

    @DELETE("api/notes/{id}/items/{itemId}")
    suspend fun deleteChecklistItem(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Path("itemId") itemId: String
    ): Response<OkResponse>

    @GET("api/projects")
    suspend fun getProjects(
        @Header("Authorization") auth: String
    ): Response<ProjectsResponse>

    @POST("api/projects")
    suspend fun createProject(
        @Header("Authorization") auth: String,
        @Body body: CreateProjectRequest
    ): Response<ProjectResponse>

    @PUT("api/projects/{id}")
    suspend fun updateProject(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: UpdateProjectRequest
    ): Response<ProjectResponse>

    @DELETE("api/projects/{id}")
    suspend fun deleteProject(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<OkResponse>

    @GET("api/labels")
    suspend fun getLabels(
        @Header("Authorization") auth: String
    ): Response<LabelsResponse>

    @POST("api/labels")
    suspend fun createLabel(
        @Header("Authorization") auth: String,
        @Body body: CreateLabelRequest
    ): Response<LabelResponse>

    @PUT("api/labels/{id}")
    suspend fun updateLabel(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: UpdateLabelRequest
    ): Response<LabelResponse>

    @DELETE("api/labels/{id}")
    suspend fun deleteLabel(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<OkResponse>

    @GET("api/search")
    suspend fun search(
        @Header("Authorization") auth: String,
        @Query("q") q: String
    ): Response<SearchResponse>

    @GET("api/schedule/upcoming")
    suspend fun getSchedule(
        @Header("Authorization") auth: String,
        @Query("days") days: Int = 7
    ): Response<ScheduleResponse>
}
