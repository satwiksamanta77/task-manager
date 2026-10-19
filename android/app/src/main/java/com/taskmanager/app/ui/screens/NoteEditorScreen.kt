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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.components.parseNoteColor
import com.taskmanager.app.ui.theme.Danger
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.ui.theme.NoteColors
import com.taskmanager.app.ui.theme.Surface
import com.taskmanager.app.ui.theme.Text
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = noteEditorText)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.togglePin() }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (state.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin",
                    tint = noteEditorText
                )
            }
            IconButton(onClick = { vm.toggleArchive() }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (state.archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                    contentDescription = "Archive",
                    tint = noteEditorText
                )
            }
            if (!state.isNew) {
                IconButton(onClick = { showDelete = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = noteEditorText)
                }
            }
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Iris)
                    .clickable { vm.save(onSaved) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            NoteColors.forEach { hex ->
                val selected = state.color.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parseNoteColor(hex))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) Iris else noteEditorText.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                        .clickable { vm.onColorChange(hex) }
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
        OutlinedTextField(
            value = state.title,
            onValueChange = vm::onTitleChange,
            placeholder = { Text("Title") },
            textStyle = MaterialTheme.typography.headlineMedium,
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
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
                .padding(horizontal = 8.dp)
        )
        state.error?.let { error ->
            Text(
                text = error,
                color = Color(0xFFB3261E),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = Surface,
            title = { Text("Delete note?", color = Text) },
            text = { Text("This note will be permanently deleted.", color = Dim) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        vm.delete(onSaved)
                    }
                ) {
                    Text("Delete", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel", color = Dim)
                }
            }
        )
    }
}
