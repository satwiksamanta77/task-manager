package com.taskmanager.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.components.EmptyState
import com.taskmanager.app.ui.components.ErrorBanner
import com.taskmanager.app.ui.components.FilterChipRow
import com.taskmanager.app.ui.components.NoteCard
import com.taskmanager.app.ui.components.PremiumTopBar
import com.taskmanager.app.ui.components.SegmentedControl
import com.taskmanager.app.ui.theme.Bg
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.viewmodel.NotesViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    vm: NotesViewModel,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onSearch: () -> Unit,
    onLabels: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.load() }

    val filtered = remember(state.notes, state.query) {
        if (state.query.isBlank()) {
            state.notes
        } else {
            state.notes.filter {
                it.title.contains(state.query, ignoreCase = true) ||
                    it.content.contains(state.query, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            PremiumTopBar(
                title = "Notes",
                subtitle = if (filtered.isEmpty()) null else "${filtered.size} notes",
                actions = {
                    IconButton(onClick = onSearch, modifier = Modifier.padding(end = 0.dp)) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Dim)
                    }
                    IconButton(onClick = onLabels, modifier = Modifier.padding(end = 0.dp)) {
                        Icon(Icons.Filled.Label, contentDescription = "Labels", tint = Dim)
                    }
                    IconButton(onClick = { vm.load() }, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Dim)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = Iris,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SegmentedControl(
                options = listOf("Notes", "Archived"),
                selectedIndex = if (state.showArchived) 1 else 0,
                onSelect = { vm.setShowArchived(it == 1) },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
            if (state.labels.isNotEmpty()) {
                FilterChipRow(
                    allLabel = "All labels",
                    items = state.labels.map { it.id to it.name },
                    selectedId = state.labelFilter,
                    onSelect = vm::setLabelFilter,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
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
            if (!state.loading && filtered.isEmpty() && state.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Filled.NoteAlt,
                        title = if (state.query.isBlank()) "No notes yet" else "No matches",
                        subtitle = if (state.query.isBlank())
                            "Tap + to capture your first note."
                        else "Try a different search."
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                    verticalItemSpacing = 10.dp,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onOpen = { onOpen(note.id) },
                            checklistProgress = state.checklistProgress[note.id]
                        )
                    }
                }
            }
        }
    }
}
