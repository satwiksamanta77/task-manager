package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.ChecklistItemDto
import com.taskmanager.app.data.api.LabelDto
import com.taskmanager.app.data.api.UpdateNoteRequest
import com.taskmanager.app.data.repository.LabelRepository
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
    val labelIds: Set<String> = emptySet(),
    val labels: List<LabelDto> = emptyList(),
    val checklist: List<ChecklistItemDto> = emptyList(),
    val isNew: Boolean = true,
    val error: String? = null
)

class NoteEditorViewModel(
    private val noteId: String?,
    private val repo: NoteRepository,
    private val labelRepo: LabelRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState(isNew = noteId == null))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        loadLabels()
        if (noteId != null) load()
    }

    private fun loadLabels() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(labels = labelRepo?.getLabels().orEmpty())
            } catch (e: Exception) {
                // labels are optional; keep going
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val id = checkNotNull(noteId) { "noteId is required to load a note" }
                val note = repo.getNote(id)
                val items = try {
                    repo.getChecklistItems(id)
                } catch (e: Exception) {
                    emptyList()
                }
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    title = note.title,
                    content = note.content,
                    color = note.color,
                    pinned = note.pinned,
                    archived = note.archived,
                    labelIds = note.labels.map { it.id }.toSet(),
                    checklist = items
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

    fun toggleLabel(id: String) {
        val current = _uiState.value.labelIds
        _uiState.value = _uiState.value.copy(
            labelIds = if (id in current) current - id else current + id
        )
    }

    // ---- Checklist ----

    fun addChecklistItem(text: String) {
        val id = noteId ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            try {
                val item = repo.addChecklistItem(id, trimmed)
                _uiState.value = _uiState.value.copy(
                    checklist = _uiState.value.checklist + item
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Could not add item"
                )
            }
        }
    }

    fun toggleChecklistItem(item: ChecklistItemDto) {
        val id = noteId ?: return
        viewModelScope.launch {
            try {
                val updated = repo.updateChecklistItem(id, item.id, checked = !item.checked)
                _uiState.value = _uiState.value.copy(
                    checklist = _uiState.value.checklist.map { if (it.id == item.id) updated else it }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Could not update item"
                )
            }
        }
    }

    fun deleteChecklistItem(itemId: String) {
        val id = noteId ?: return
        viewModelScope.launch {
            try {
                repo.deleteChecklistItem(id, itemId)
                _uiState.value = _uiState.value.copy(
                    checklist = _uiState.value.checklist.filterNot { it.id == itemId }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Could not delete item"
                )
            }
        }
    }

    // ---- Save / delete ----

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            try {
                if (state.isNew) {
                    repo.createNote(
                        title = state.title.ifBlank { null },
                        content = state.content.ifBlank { null },
                        color = state.color,
                        labelIds = state.labelIds.toList()
                    )
                } else {
                    repo.updateNote(
                        checkNotNull(noteId) { "noteId is required to update a note" },
                        UpdateNoteRequest(
                            title = state.title,
                            content = state.content,
                            color = state.color,
                            pinned = state.pinned,
                            archived = state.archived,
                            label_ids = state.labelIds.toList()
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

    /** Soft-delete (moves to trash per api-contract v2). */
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
