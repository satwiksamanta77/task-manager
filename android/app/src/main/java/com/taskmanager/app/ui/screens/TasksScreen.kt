package com.taskmanager.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
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
import com.taskmanager.app.ui.components.PremiumTopBar
import com.taskmanager.app.ui.components.SegmentedControl
import com.taskmanager.app.ui.components.TaskCard
import com.taskmanager.app.ui.theme.Bg
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.viewmodel.TasksViewModel

@Composable
fun TasksScreen(
    vm: TasksViewModel,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onSearch: () -> Unit,
    onProjects: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.load() }

    val openCount = state.tasks.count { it.status != "done" }
    val doneCount = state.tasks.size - openCount
    val selectedIndex = when (state.filter) {
        "todo" -> 1
        "done" -> 2
        else -> 0
    }
    val projectById = remember(state.projects) { state.projects.associateBy { it.id } }

    Scaffold(
        containerColor = Bg,
        topBar = {
            PremiumTopBar(
                title = "Tasks",
                subtitle = if (state.tasks.isEmpty()) null
                else "$openCount open · $doneCount done",
                actions = {
                    IconButton(onClick = onSearch, modifier = Modifier.padding(end = 0.dp)) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Dim)
                    }
                    IconButton(onClick = onProjects, modifier = Modifier.padding(end = 0.dp)) {
                        Icon(Icons.Filled.Folder, contentDescription = "Projects", tint = Dim)
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
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SegmentedControl(
                options = listOf("All", "To do", "Done"),
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    vm.setFilter(when (index) { 1 -> "todo"; 2 -> "done"; else -> null })
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
            if (state.projects.isNotEmpty()) {
                FilterChipRow(
                    allLabel = "All projects",
                    items = state.projects.map { it.id to it.name },
                    selectedId = state.projectFilter,
                    onSelect = vm::setProjectFilter,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (state.labels.isNotEmpty()) {
                FilterChipRow(
                    allLabel = "All labels",
                    items = state.labels.map { it.id to it.name },
                    selectedId = state.labelFilter,
                    onSelect = vm::setLabelFilter,
                    modifier = Modifier.padding(bottom = 8.dp),
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            Icons.Filled.Label,
                            contentDescription = null,
                            tint = Dim,
                            modifier = Modifier.padding(end = 0.dp)
                        )
                    }
                )
            }
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
            if (!state.loading && state.tasks.isEmpty() && state.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Filled.Checklist,
                        title = "No tasks yet",
                        subtitle = "Tap + to create your first task and stay on top of your day."
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    items(state.tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { vm.toggleDone(task) },
                            onOpen = { onOpen(task.id) },
                            project = task.project_id?.let { projectById[it] }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }
    }
}
