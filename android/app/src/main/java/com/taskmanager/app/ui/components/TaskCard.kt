package com.taskmanager.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskmanager.app.data.api.TaskDto
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.ui.theme.Text
import com.taskmanager.app.ui.theme.soft

@Composable
fun TaskCard(
    task: TaskDto,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    val done = task.status == "done"
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumCheckbox(checked = done, onToggle = onToggle)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) Dim else Text,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriorityPill(priority = task.priority)
                    if (!task.due_at.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        DueChip(iso = task.due_at)
                    }
                }
                val subtasks = task.subtasks
                if (!subtasks.isNullOrEmpty()) {
                    val doneCount = subtasks.count { it.done }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { doneCount.toFloat() / subtasks.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Iris,
                        trackColor = Iris.soft(0.18f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$doneCount of ${subtasks.size} subtasks",
                        style = MaterialTheme.typography.labelMedium,
                        color = Dim
                    )
                }
            }
        }
    }
}
