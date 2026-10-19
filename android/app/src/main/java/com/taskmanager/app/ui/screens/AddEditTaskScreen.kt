package com.taskmanager.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.util.capitalizeFirst
import com.taskmanager.app.viewmodel.AddEditTaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    vm: AddEditTaskViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var priorityMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New task" else "Edit task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { vm.save(onSaved) }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = vm::onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box {
                OutlinedButton(onClick = { priorityMenu = true }) {
                    Text("Priority: ${capitalizeFirst(state.priority)}")
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
                                vm.onPriorityChange(p)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.dueAt,
                onValueChange = vm::onDueAtChange,
                label = { Text("Due date (ISO, e.g. 2026-10-21T10:00:00Z)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
