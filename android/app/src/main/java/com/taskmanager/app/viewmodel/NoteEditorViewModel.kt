package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.UpdateNoteRequest
import com.taskmanager.app.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val loading: Boolean = false,
    val title: String = "",
    val content: String = "",
    val color: String = "#FFFFFF",
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val isNew: Boolean = true,
    val error: String? = null
)

class NoteEditorViewModel(
    private val noteId: String?,
    private val repo: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState(isNew = noteId == null))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (noteId != null) load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val note = repo.getNote(noteId!!)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    title = note.title,
                    content = note.content,
                    color = note.color,
                    pinned = note.pinned,
                    archived = note.archived
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load note"
                )
            }
        }
    }

    fun onTitleChange(v: String) { _uiState.value = _uiState.value.copy(title = v) }
    fun onContentChange(v: String) { _uiState.value = _uiState.value.copy(content = v) }
    fun onColorChange(v: String) { _uiState.value = _uiState.value.copy(color = v) }
    fun togglePin() { _uiState.value = _uiState.value.copy(pinned = !_uiState.value.pinned) }
    fun toggleArchive() { _uiState.value = _uiState.value.copy(archived = !_uiState.value.archived) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            try {
                if (state.isNew) {
                    repo.createNote(
                        title = state.title.ifBlank { null },
                        content = state.content.ifBlank { null },
                        color = state.color
                    )
                } else {
                    repo.updateNote(
                        noteId!!,
                        UpdateNoteRequest(
                            title = state.title,
                            content = state.content,
                            color = state.color,
                            pinned = state.pinned,
                            archived = state.archived
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(loading = false)
                onSaved()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not save note"
                )
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = noteId ?: run { onDeleted(); return }
        viewModelScope.launch {
            try {
                repo.deleteNote(id)
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not delete note")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
