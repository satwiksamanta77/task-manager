package com.taskmanager.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.components.EmptyState
import com.taskmanager.app.ui.components.ErrorBanner
import com.taskmanager.app.ui.components.PremiumCard
import com.taskmanager.app.ui.components.PremiumTopBar
import com.taskmanager.app.ui.components.PriorityPill
import com.taskmanager.app.ui.theme.Bg
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.ui.theme.Text
import com.taskmanager.app.ui.util.formatDayHeader
import com.taskmanager.app.viewmodel.ScheduleViewModel

@OptIn(ExperimentalFoundationApi::class)
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
        containerColor = Bg,
        topBar = {
            PremiumTopBar(
                title = "Schedule",
                subtitle = if (state.items.isEmpty()) "Next 7 days"
                else "${state.items.size} upcoming in the next 7 days",
                actions = {
                    IconButton(onClick = { vm.load() }, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Dim)
                    }
                }
            )
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
            if (!state.loading && state.items.isEmpty() && state.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Filled.CalendarMonth,
                        title = "All clear",
                        subtitle = "Nothing is scheduled in the next 7 days. Enjoy the calm."
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (date, items) ->
                        stickyHeader(key = "header-$date") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Bg)
                                    .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 6.dp)
                            ) {
                                Text(
                                    text = formatDayHeader(date),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Iris
                                )
                            }
                        }
                        items(items, key = { it.id }) { item ->
                            PremiumCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Iris.copy(alpha = 0.75f))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Text
                                        )
                                        val time = (item.due_at ?: item.reminder_at)
                                            ?.take(16)?.substringAfter("T")
                                        if (!time.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = time,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Dim
                                            )
                                        }
                                    }
                                    PriorityPill(priority = item.priority)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
