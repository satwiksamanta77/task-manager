package com.taskmanager.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.components.PriorityPill
import com.taskmanager.app.ui.theme.Border
import com.taskmanager.app.ui.theme.Surface
import com.taskmanager.app.ui.util.capitalizeFirst
import com.taskmanager.app.ui.util.formatDueDate
import com.taskmanager.app.viewmodel.TaskDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    vm: TaskDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var newSubtask by remember { mutableStateOf("") }
    var priorityMenu by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit task")
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete task")
                    }
                }
            )
        }
    ) { padding ->
        val task = state.task
        when {
            state.loading && task == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            task != null -> {
                val done = task.status == "done"
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = done,
                            onCheckedChange = { vm.toggleDone() }
                        )
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleLarge,
                            textDecoration = if (done) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!task.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PriorityPill(priority = task.priority)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Box {
                            OutlinedButton(onClick = { priorityMenu = true }) {
                                Text("Change priority")
                            }
                            DropdownMenu(
                                expanded = priorityMenu,
                                onDismissRequest = { priorityMenu = false }
                            ) {
                                listOf("low", "medium", "high").forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(capitalizeFirst(p)) },
                                        onClick = {
                                            priorityMenu = false
                                            vm.setPriority(p)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    val due = formatDueDate(task.due_at)
                    if (due.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Due $due",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Subtasks",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Border),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            task.subtasks.orEmpty().forEach { subtask ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = subtask.done,
                                        onCheckedChange = { vm.toggleSubtask(subtask) }
                                    )
                                    Text(
                                        text = subtask.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textDecoration = if (subtask.done) TextDecoration.LineThrough else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { vm.deleteSubtask(subtask.id) }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete subtask"
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newSubtask,
                                    onValueChange = { newSubtask = it },
                                    label = { Text("New subtask") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.padding(4.dp))
                                Button(
                                    onClick = {
                                        if (newSubtask.isNotBlank()) {
                                            vm.addSubtask(newSubtask.trim())
                                            newSubtask = ""
                                        }
                                    }
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                    state.error?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete task?") },
            text = { Text("This will permanently delete the task and its subtasks.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        vm.deleteTask(onBack)
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
