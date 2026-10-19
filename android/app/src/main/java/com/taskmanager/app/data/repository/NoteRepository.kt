package com.taskmanager.app.data.repository

import com.taskmanager.app.data.api.ApiService
import com.taskmanager.app.data.api.CreateNoteRequest
import com.taskmanager.app.data.api.NoteDto
import com.taskmanager.app.data.api.UpdateNoteRequest
import com.taskmanager.app.data.api.unwrap
import com.taskmanager.app.data.local.SessionManager

class NoteRepository(
    private val api: ApiService,
    session: SessionManager
) : BaseRepository(session) {

    suspend fun getNotes(archived: Boolean = false): List<NoteDto> =
        unwrap(api.getNotes(authHeader(), archived)).notes

    suspend fun createNote(title: String?, content: String?, color: String?): NoteDto =
        unwrap(
            api.createNote(
                authHeader(),
                CreateNoteRequest(
                    title = title?.ifBlank { null },
                    content = content?.ifBlank { null },
                    color = color
                )
            )
        ).note

    suspend fun getNote(id: String): NoteDto =
        unwrap(api.getNote(authHeader(), id)).note

    suspend fun updateNote(id: String, body: UpdateNoteRequest): NoteDto =
        unwrap(api.updateNote(authHeader(), id, body)).note

    suspend fun deleteNote(id: String) {
        unwrap(api.deleteNote(authHeader(), id))
    }
}
