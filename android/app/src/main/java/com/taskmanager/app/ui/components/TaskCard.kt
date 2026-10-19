package com.taskmanager.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.taskmanager.app.data.api.TaskDto
import com.taskmanager.app.ui.theme.Border
import com.taskmanager.app.ui.theme.Surface
import com.taskmanager.app.ui.util.formatDueDate

@Composable
fun TaskCard(
    task: TaskDto,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    val done = task.status == "done"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onOpen() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = done,
                onCheckedChange = { onToggle() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (done) TextDecoration.LineThrough else null
                )
                val due = formatDueDate(task.due_at)
                if (due.isNotEmpty()) {
                    Text(
                        text = "Due $due",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val subtasks = task.subtasks
                if (!subtasks.isNullOrEmpty()) {
                    val doneCount = subtasks.count { it.done }
                    Text(
                        text = "$doneCount/${subtasks.size} subtasks",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            PriorityPill(priority = task.priority)
        }
    }
}
