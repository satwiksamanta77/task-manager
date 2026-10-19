package com.taskmanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.data.api.NoteDto
import com.taskmanager.app.data.api.TaskDto
import com.taskmanager.app.ui.components.EmptyState
import com.taskmanager.app.ui.components.ErrorBanner
import com.taskmanager.app.ui.components.PremiumCard
import com.taskmanager.app.ui.components.PremiumTopBar
import com.taskmanager.app.ui.components.SectionLabel
import com.taskmanager.app.ui.theme.Bg
import com.taskmanager.app.ui.theme.Border
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.ui.theme.Surface2
import com.taskmanager.app.ui.theme.Text
import com.taskmanager.app.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    vm: SearchViewModel,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenNote: (String) -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Bg,
        topBar = { PremiumTopBar(title = "Search", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Dim) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Dim)
                        }
                    }
                },
                placeholder = { Text("Search tasks and notes", color = Dim) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Iris,
                    unfocusedBorderColor = Border,
                    focusedTextColor = Text,
                    unfocusedTextColor = Text,
                    cursorColor = Iris,
                    focusedContainerColor = Surface2.copy(alpha = 0.6f),
                    unfocusedContainerColor = Surface2.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (state.loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Iris,
                    trackColor = Iris.copy(alpha = 0.15f)
                )
            }
            state.error?.let { error ->
                ErrorBanner(
                    text = error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            when {
                state.query.trim().length < 2 && !state.searched -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = "Search everything",
                            subtitle = "Type at least 2 characters to search tasks and notes."
                        )
                    }
                }
                !state.loading && state.searched &&
                    state.tasks.isEmpty() && state.notes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = "No results",
                            subtitle = "Nothing matched \"${state.query}\"."
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (state.tasks.isNotEmpty()) {
                            item {
                                SectionLabel(
                                    "Tasks · ${state.tasks.size}",
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }
                            items(state.tasks, key = { "t${it.id}" }) { task ->
                                SearchTaskRow(task = task, onClick = { onOpenTask(task.id) })
                            }
                        }
                        if (state.notes.isNotEmpty()) {
                            item {
                                SectionLabel(
                                    "Notes · ${state.notes.size}",
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }
                            items(state.notes, key = { "n${it.id}" }) { note ->
                                SearchNoteRow(note = note, onClick = { onOpenNote(note.id) })
                            }
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTaskRow(task: TaskDto, onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Checklist,
                contentDescription = null,
                tint = Iris,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Dim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchNoteRow(note: NoteDto, onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.NoteAlt,
                contentDescription = null,
                tint = Iris,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = Dim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
