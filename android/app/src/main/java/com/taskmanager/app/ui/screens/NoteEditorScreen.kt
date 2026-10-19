package com.taskmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.components.parseNoteColor
import com.taskmanager.app.ui.theme.NoteColors
import com.taskmanager.app.ui.theme.Primary
import com.taskmanager.app.viewmodel.NoteEditorViewModel

private val noteEditorText = Color(0xFF1A1D26)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    vm: NoteEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showDelete by remember { mutableStateOf(false) }
    val bg = remember(state.color) { parseNoteColor(state.color) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedTextColor = noteEditorText,
        unfocusedTextColor = noteEditorText,
        cursorColor = noteEditorText,
        focusedPlaceholderColor = noteEditorText.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = noteEditorText.copy(alpha = 0.5f)
    )

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New note" else "Edit note", color = noteEditorText) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = noteEditorText)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.togglePin() }) {
                        Icon(
                            imageVector = if (state.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = noteEditorText
                        )
                    }
                    IconButton(onClick = { vm.toggleArchive() }) {
                        Icon(
                            imageVector = if (state.archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                            contentDescription = "Archive",
                            tint = noteEditorText
                        )
                    }
                    if (!state.isNew) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = noteEditorText)
                        }
                    }
                    IconButton(onClick = { vm.save(onSaved) }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save", tint = noteEditorText)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row {
                NoteColors.forEach { hex ->
                    val selected = state.color.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(parseNoteColor(hex))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Primary else noteEditorText.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { vm.onColorChange(hex) }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.content,
                onValueChange = vm::onContentChange,
                placeholder = { Text("Note") },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            state.error?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        vm.delete(onSaved)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
