package com.taskmanager.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.components.ErrorBanner
import com.taskmanager.app.ui.components.PremiumTextField
import com.taskmanager.app.ui.components.PremiumTopBar
import com.taskmanager.app.ui.components.SectionLabel
import com.taskmanager.app.ui.components.SegmentedControl
import com.taskmanager.app.ui.theme.Bg
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.viewmodel.AddEditTaskViewModel

@Composable
fun AddEditTaskScreen(
    vm: AddEditTaskViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val priorities = listOf("low", "medium", "high")
    val priorityLabels = listOf("Low", "Medium", "High")
    val priorityIndex = priorities.indexOf(state.priority).coerceAtLeast(0)

    Scaffold(
        containerColor = Bg,
        topBar = {
            PremiumTopBar(
                title = if (state.isNew) "New task" else "Edit task",
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = { vm.save(onSaved) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save", color = Iris, style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            SectionLabel("Title")
            Spacer(modifier = Modifier.height(8.dp))
            PremiumTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                label = "What needs doing?"
            )
            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("Description")
            Spacer(modifier = Modifier.height(8.dp))
            PremiumTextField(
                value = state.description,
                onValueChange = vm::onDescriptionChange,
                label = "Add details (optional)",
                singleLine = false,
                minLines = 3
            )
            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("Priority")
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedControl(
                options = priorityLabels,
                selectedIndex = priorityIndex,
                onSelect = { vm.onPriorityChange(priorities[it]) }
            )
            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("Due date")
            Spacer(modifier = Modifier.height(8.dp))
            PremiumTextField(
                value = state.dueAt,
                onValueChange = vm::onDueAtChange,
                label = "2026-10-21T10:00:00Z"
            )
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                ErrorBanner(text = error)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
