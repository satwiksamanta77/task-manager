package com.taskmanager.app.data.repository

import com.taskmanager.app.data.api.ApiService
import com.taskmanager.app.data.api.ChecklistItemDto
import com.taskmanager.app.data.api.CreateChecklistItemRequest
import com.taskmanager.app.data.api.CreateNoteRequest
import com.taskmanager.app.data.api.NoteDto
import com.taskmanager.app.data.api.UpdateChecklistItemRequest
import com.taskmanager.app.data.api.UpdateNoteRequest
import com.taskmanager.app.data.api.unwrap
import com.taskmanager.app.data.local.SessionManager

class NoteRepository(
    private val api: ApiService,
    session: SessionManager
) : BaseRepository(session) {

    suspend fun getNotes(
        archived: Boolean = false,
        labelId: String? = null,
        trashed: Boolean = false
    ): List<NoteDto> =
        unwrap(api.getNotes(authHeader(), archived, labelId, trashed.takeIf { it })).notes

    suspend fun createNote(
        title: String?,
        content: String?,
        color: String?,
        labelIds: List<String> = emptyList()
    ): NoteDto =
        unwrap(
            api.createNote(
                authHeader(),
                CreateNoteRequest(
                    title = title?.ifBlank { null },
                    content = content?.ifBlank { null },
                    color = color,
                    label_ids = labelIds.takeIf { it.isNotEmpty() }
                )
            )
        ).note

    suspend fun getNote(id: String): NoteDto =
        unwrap(api.getNote(authHeader(), id)).note

    suspend fun updateNote(id: String, body: UpdateNoteRequest): NoteDto =
        unwrap(api.updateNote(authHeader(), id, body)).note

    suspend fun deleteNote(id: String, permanent: Boolean = false) {
        unwrap(api.deleteNote(authHeader(), id, permanent.takeIf { it }))
    }

    suspend fun restoreNote(id: String): NoteDto =
        unwrap(api.restoreNote(authHeader(), id)).note

    suspend fun getChecklistItems(noteId: String): List<ChecklistItemDto> =
        unwrap(api.getChecklistItems(authHeader(), noteId)).items

    suspend fun addChecklistItem(noteId: String, text: String): ChecklistItemDto =
        unwrap(api.createChecklistItem(authHeader(), noteId, CreateChecklistItemRequest(text))).item

    suspend fun updateChecklistItem(
        noteId: String,
        itemId: String,
        text: String? = null,
        checked: Boolean? = null,
        position: Int? = null
    ): ChecklistItemDto =
        unwrap(
            api.updateChecklistItem(
                authHeader(),
                noteId,
                itemId,
                UpdateChecklistItemRequest(text, checked, position)
            )
        ).item

    suspend fun deleteChecklistItem(noteId: String, itemId: String) {
        unwrap(api.deleteChecklistItem(authHeader(), noteId, itemId))
    }
}
