package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.NoteDto
import com.taskmanager.app.data.api.TaskDto
import com.taskmanager.app.data.repository.SearchRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val tasks: List<TaskDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val searched: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(private val repo: SearchRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        queryFlow
            .debounce(350)
            .distinctUntilChanged()
            .flatMapLatest { q ->
                flow {
                    if (q.trim().length < 2) {
                        emit(SearchUiState(query = q))
                    } else {
                        emit(_uiState.value.copy(loading = true, error = null))
                        val result = repo.search(q)
                        emit(
                            SearchUiState(
                                query = q,
                                tasks = result.tasks,
                                notes = result.notes,
                                searched = true
                            )
                        )
                    }
                }.catch { e ->
                    emit(
                        _uiState.value.copy(
                            loading = false,
                            error = e.message ?: "Search failed"
                        )
                    )
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(q: String) {
        _uiState.value = _uiState.value.copy(query = q)
        queryFlow.value = q
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
