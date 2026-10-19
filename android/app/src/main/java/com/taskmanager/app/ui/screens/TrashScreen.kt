package com.taskmanager.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.components.EmptyState
import com.taskmanager.app.ui.components.ErrorBanner
import com.taskmanager.app.ui.components.PremiumCard
import com.taskmanager.app.ui.components.PremiumTopBar
import com.taskmanager.app.ui.components.SectionLabel
import com.taskmanager.app.ui.theme.Bg
import com.taskmanager.app.ui.theme.Danger
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.ui.theme.Surface
import com.taskmanager.app.ui.theme.Text
import com.taskmanager.app.viewmodel.TrashViewModel

@Composable
fun TrashScreen(
    vm: TrashViewModel,
    onBack: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showEmptyConfirm by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    val total = state.tasks.size + state.notes.size

    Scaffold(
        containerColor = Bg,
        topBar = {
            PremiumTopBar(
                title = "Trash",
                subtitle = if (total == 0) null else "$total deleted item${if (total == 1) "" else "s"}",
                onBack = onBack,
                actions = {
                    if (total > 0) {
                        IconButton(
                            onClick = { showEmptyConfirm = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.DeleteForever,
                                contentDescription = "Empty trash",
                                tint = Danger
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
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
            if (!state.loading && total == 0 && state.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Filled.DeleteOutline,
                        title = "Trash is empty",
                        subtitle = "Deleted tasks and notes will appear here."
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (state.tasks.isNotEmpty()) {
                        item {
                            SectionLabel(
                                "Tasks · ${state.tasks.size}",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                        items(state.tasks, key = { "t${it.id}" }) { task ->
                            TrashRow(
                                title = task.title,
                                icon = Icons.Filled.Checklist,
                                onRestore = { vm.restoreTask(task.id) },
                                onDeleteForever = { confirmDelete = task.id to true }
                            )
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
                            TrashRow(
                                title = note.title.ifBlank { "Untitled" },
                                icon = Icons.Filled.NoteAlt,
                                onRestore = { vm.restoreNote(note.id) },
                                onDeleteForever = { confirmDelete = note.id to false }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            containerColor = Surface,
            title = { Text("Empty trash?", color = Text) },
            text = {
                Text(
                    "All $total deleted items will be permanently removed. This cannot be undone.",
                    color = Dim
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyConfirm = false
                        vm.emptyTrash()
                    }
                ) {
                    Text("Empty trash", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text("Cancel", color = Dim)
                }
            }
        )
    }

    confirmDelete?.let { (id, isTask) ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = Surface,
            title = { Text("Delete forever?", color = Text) },
            text = {
                Text(
                    "This item will be permanently deleted. This cannot be undone.",
                    color = Dim
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = null
                        if (isTask) vm.deleteTaskForever(id) else vm.deleteNoteForever(id)
                    }
                ) {
                    Text("Delete forever", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text("Cancel", color = Dim)
                }
            }
        )
    }
}

@Composable
private fun TrashRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Dim)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Dim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRestore, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Filled.Restore, contentDescription = "Restore", tint = Iris)
            }
            IconButton(onClick = onDeleteForever, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Filled.DeleteForever, contentDescription = "Delete forever", tint = Danger)
            }
        }
    }
}
