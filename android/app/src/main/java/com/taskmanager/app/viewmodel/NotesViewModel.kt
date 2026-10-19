package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.ChecklistItemDto
import com.taskmanager.app.data.api.LabelDto
import com.taskmanager.app.data.api.NoteDto
import com.taskmanager.app.data.repository.LabelRepository
import com.taskmanager.app.data.repository.NoteRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotesUiState(
    val loading: Boolean = false,
    val notes: List<NoteDto> = emptyList(),
    val labels: List<LabelDto> = emptyList(),
    val labelFilter: String? = null,
    val query: String = "",
    val showArchived: Boolean = false,
    /** noteId -> (done, total) checklist progress */
    val checklistProgress: Map<String, Pair<Int, Int>> = emptyMap(),
    val error: String? = null
)

class NotesViewModel(
    private val repo: NoteRepository,
    private val labelRepo: LabelRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState(loading = true))
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val s = _uiState.value
            _uiState.value = s.copy(loading = true, error = null)
            try {
                val notes = repo.getNotes(archived = s.showArchived, labelId = s.labelFilter)
                val labels = try {
                    labelRepo?.getLabels().orEmpty()
                } catch (e: Exception) { emptyList() }
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    notes = notes,
                    labels = labels
                )
                loadChecklistProgress(notes)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load notes"
                )
            }
        }
    }

    /** Best-effort progress for visible notes; failures are ignored per note. */
    private fun loadChecklistProgress(notes: List<NoteDto>) {
        viewModelScope.launch {
            val results = notes.map { note ->
                async {
                    note.id to try {
                        val items = repo.getChecklistItems(note.id)
                        if (items.isEmpty()) null
                        else items.count(ChecklistItemDto::checked) to items.size
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().mapNotNull { (id, progress) -> progress?.let { id to it } }.toMap()
            _uiState.value = _uiState.value.copy(checklistProgress = results)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun setShowArchived(show: Boolean) {
        if (_uiState.value.showArchived == show) return
        _uiState.value = _uiState.value.copy(showArchived = show)
        load()
    }

    fun setLabelFilter(labelId: String?) {
        if (_uiState.value.labelFilter == labelId) return
        _uiState.value = _uiState.value.copy(labelFilter = labelId)
        load()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
