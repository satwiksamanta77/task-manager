package com.taskmanager.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.taskmanager.app.data.api.ProjectDto
import com.taskmanager.app.ui.components.EmptyState
import com.taskmanager.app.ui.components.ErrorBanner
import com.taskmanager.app.ui.components.PremiumCard
import com.taskmanager.app.ui.components.PremiumTopBar
import com.taskmanager.app.ui.components.TagEditDialog
import com.taskmanager.app.ui.theme.Bg
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.ui.theme.Text
import com.taskmanager.app.ui.theme.soft
import com.taskmanager.app.viewmodel.ProjectsViewModel

@Composable
fun ProjectsScreen(
    vm: ProjectsViewModel,
    onBack: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProjectDto?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Bg,
        topBar = {
            PremiumTopBar(
                title = "Projects",
                subtitle = if (state.projects.isEmpty()) null else "${state.projects.size} projects",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = Iris,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add project")
            }
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
            if (!state.loading && state.projects.isEmpty() && state.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Filled.Folder,
                        title = "No projects yet",
                        subtitle = "Group related tasks with projects."
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(state.projects, key = { it.id }) { project ->
                        ProjectRow(
                            project = project,
                            onClick = { editing = project }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }
    }

    if (showCreate) {
        TagEditDialog(
            title = "New project",
            initialName = "",
            initialColor = null,
            onDismiss = { showCreate = false },
            onSave = { name, color -> vm.create(name, color) }
        )
    }
    editing?.let { project ->
        TagEditDialog(
            title = "Edit project",
            initialName = project.name,
            initialColor = project.color,
            onDismiss = { editing = null },
            onSave = { name, color -> vm.update(project.id, name, color) },
            onDelete = { vm.delete(project.id) }
        )
    }
}

@Composable
private fun ProjectRow(project: ProjectDto, onClick: () -> Unit) {
    val color = remember(project.color) {
        project.color?.let {
            try {
                Color(android.graphics.Color.parseColor(it))
            } catch (e: Exception) {
                Iris
            }
        } ?: Iris
    }
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.soft(0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Text
                )
                Text(
                    text = "Tap to edit",
                    style = MaterialTheme.typography.bodySmall,
                    color = Dim,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
