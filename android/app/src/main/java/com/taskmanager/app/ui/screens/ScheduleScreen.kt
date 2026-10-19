package com.taskmanager.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.components.PriorityPill
import com.taskmanager.app.ui.theme.Border
import com.taskmanager.app.ui.theme.Surface
import com.taskmanager.app.ui.util.formatDueDate
import com.taskmanager.app.viewmodel.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(vm: ScheduleViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.load() }

    val grouped = remember(state.items) {
        state.items
            .groupBy { (it.due_at ?: it.reminder_at)?.take(10) ?: "No date" }
            .toSortedMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule") },
                actions = {
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            if (!state.loading && state.items.isEmpty() && state.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nothing scheduled in the next 7 days.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (date, items) ->
                        item(key = "header-$date") {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 12.dp,
                                    end = 16.dp,
                                    bottom = 4.dp
                                )
                            )
                        }
                        items(items, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Border),
                                colors = CardDefaults.cardColors(containerColor = Surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        val whenText = formatDueDate(item.due_at ?: item.reminder_at)
                                        if (whenText.isNotEmpty()) {
                                            Text(
                                                text = whenText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    PriorityPill(priority = item.priority)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
