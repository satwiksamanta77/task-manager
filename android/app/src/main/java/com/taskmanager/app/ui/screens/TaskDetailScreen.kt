@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.components.DueChip
import com.taskmanager.app.ui.components.EmptyState
import com.taskmanager.app.ui.components.ErrorBanner
import com.taskmanager.app.ui.components.LabelPill
import com.taskmanager.app.ui.components.PremiumCard
import com.taskmanager.app.ui.components.PremiumCheckbox
import com.taskmanager.app.ui.components.PremiumTextField
import com.taskmanager.app.ui.components.PremiumTopBar
import com.taskmanager.app.ui.components.PriorityPill
import com.taskmanager.app.ui.components.RecurrenceBadge
import com.taskmanager.app.ui.components.SectionLabel
import com.taskmanager.app.ui.theme.Bg
import com.taskmanager.app.ui.theme.Danger
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.ui.theme.Surface
import com.taskmanager.app.ui.theme.Text
import com.taskmanager.app.viewmodel.TaskDetailViewModel

@Composable
fun TaskDetailScreen(
    vm: TaskDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var newSubtask by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(containerColor = Bg) { padding ->
        val task = state.task
        Column(modifier = Modifier.padding(padding)) {
            PremiumTopBar(
                title = "Details",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onEdit, modifier = Modifier.padding(end = 0.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit task", tint = Dim)
                    }
                    IconButton(onClick = { showDelete = true }, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = Dim)
                    }
                }
            )
            when {
                state.loading && task == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Iris)
                    }
                }
                task != null -> {
                    val done = task.status == "done"
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PremiumCheckbox(
                                        checked = done,
                                        onToggle = { vm.toggleDone() },
                                        modifier = Modifier.padding(start = 0.dp)
                                    )
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = if (done) Dim else Text,
                                        textDecoration = if (done) TextDecoration.LineThrough else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (state.projectName != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.projectName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Iris
                                    )
                                }
                                if (!task.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Dim
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PriorityPill(priority = task.priority)
                                    if (!task.due_at.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        DueChip(iso = task.due_at)
                                    }
                                    RecurrenceBadge(
                                        recurrence = task.recurrence,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                if (task.labels.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    androidx.compose.foundation.layout.FlowRow(
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                                            6.dp
                                        ),
                                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                                            6.dp
                                        )
                                    ) {
                                        task.labels.forEach { label ->
                                            LabelPill(label = label)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("Subtasks", modifier = Modifier.weight(1f))
                            val subs = task.subtasks.orEmpty()
                            if (subs.isNotEmpty()) {
                                Text(
                                    text = "${subs.count { it.done }}/${subs.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Dim
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)) {
                                val subs = task.subtasks.orEmpty()
                                if (subs.isEmpty()) {
                                    EmptyState(
                                        icon = Icons.Filled.PlaylistAddCheck,
                                        title = "No subtasks",
                                        subtitle = "Break this task down into smaller steps.",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    subs.forEach { subtask ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            PremiumCheckbox(
                                                checked = subtask.done,
                                                onToggle = { vm.toggleSubtask(subtask) }
                                            )
                                            Text(
                                                text = subtask.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (subtask.done) Dim else Text,
                                                textDecoration = if (subtask.done) TextDecoration.LineThrough else null,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { vm.deleteSubtask(subtask.id) }) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = "Delete subtask",
                                                    tint = Dim
                                                )
                                            }
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    PremiumTextField(
                                        value = newSubtask,
                                        onValueChange = { newSubtask = it },
                                        label = "New subtask",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Button(
                                        onClick = {
                                            if (newSubtask.isNotBlank()) {
                                                vm.addSubtask(newSubtask.trim())
                                                newSubtask = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Iris,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.height(54.dp)
                                    ) {
                                        Text("Add", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                        state.error?.let { error ->
                            Spacer(modifier = Modifier.height(12.dp))
                            ErrorBanner(text = error)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = Surface,
            title = { Text("Move to trash?", color = Text) },
            text = { Text("You can restore this task from Trash later.", color = Dim) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        vm.deleteTask(onBack)
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
