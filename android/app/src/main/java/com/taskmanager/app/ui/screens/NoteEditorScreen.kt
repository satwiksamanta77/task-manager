@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.taskmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PlaylistAddCheck
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.data.api.ChecklistItemDto
import com.taskmanager.app.data.api.LabelDto
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
    var newItem by remember { mutableStateOf("") }
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
                    Icon(Icons.Filled.Delete, contentDescription = "Move to trash", tint = noteEditorText)
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
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
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
                    .padding(horizontal = 8.dp)
            )
            if (state.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "LABELS",
                    style = MaterialTheme.typography.labelSmall,
                    color = noteEditorText.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                EditorLabelChips(
                    labels = state.labels,
                    selectedIds = state.labelIds,
                    onToggle = vm::toggleLabel,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            ChecklistSection(
                items = state.checklist,
                isNew = state.isNew,
                newItem = newItem,
                onNewItemChange = { newItem = it },
                onAdd = {
                    vm.addChecklistItem(newItem)
                    newItem = ""
                },
                onToggle = vm::toggleChecklistItem,
                onDelete = vm::deleteChecklistItem,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            state.error?.let { error ->
                Text(
                    text = error,
                    color = Danger,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = Surface,
            title = { Text("Move to trash?", color = Text) },
            text = { Text("You can restore this note from Trash later.", color = Dim) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        vm.delete(onSaved)
                    }
                ) {
                    Text("Move to trash", color = Danger)
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

/** Label multi-select chips tinted for the note editor's colored background. */
@Composable
private fun EditorLabelChips(
    labels: List<LabelDto>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            val selected = label.id in selectedIds
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) noteEditorText.copy(alpha = 0.14f)
                        else noteEditorText.copy(alpha = 0.06f)
                    )
                    .border(
                        1.dp,
                        if (selected) noteEditorText.copy(alpha = 0.7f)
                        else noteEditorText.copy(alpha = 0.2f),
                        RoundedCornerShape(50)
                    )
                    .clickable { onToggle(label.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Label,
                    contentDescription = null,
                    tint = noteEditorText.copy(alpha = if (selected) 0.9f else 0.55f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = noteEditorText.copy(alpha = if (selected) 0.95f else 0.65f)
                )
            }
        }
    }
}

@Composable
private fun ChecklistSection(
    items: List<ChecklistItemDto>,
    isNew: Boolean,
    newItem: String,
    onNewItemChange: (String) -> Unit,
    onAdd: () -> Unit,
    onToggle: (ChecklistItemDto) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.PlaylistAddCheck,
                contentDescription = null,
                tint = noteEditorText.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CHECKLIST",
                style = MaterialTheme.typography.labelSmall,
                color = noteEditorText.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
            if (items.isNotEmpty()) {
                Text(
                    text = "${items.count { it.checked }}/${items.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = noteEditorText.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (isNew) {
            Text(
                text = "Save the note first to add checklist items.",
                style = MaterialTheme.typography.bodySmall,
                color = noteEditorText.copy(alpha = 0.55f)
            )
        } else {
            items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onToggle(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (item.checked) noteEditorText.copy(alpha = 0.85f)
                                    else Color.Transparent
                                )
                                .border(
                                    2.dp,
                                    noteEditorText.copy(alpha = 0.7f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.checked) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = noteEditorText.copy(alpha = if (item.checked) 0.55f else 0.9f),
                        textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onDelete(item.id) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Delete item",
                            tint = noteEditorText.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = onNewItemChange,
                    placeholder = {
                        Text(
                            "New item",
                            color = noteEditorText.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = noteEditorText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = noteEditorText.copy(alpha = 0.6f),
                        unfocusedBorderColor = noteEditorText.copy(alpha = 0.3f),
                        focusedContainerColor = noteEditorText.copy(alpha = 0.06f),
                        unfocusedContainerColor = noteEditorText.copy(alpha = 0.06f),
                        cursorColor = noteEditorText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = onAdd,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add item",
                                tint = noteEditorText
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
