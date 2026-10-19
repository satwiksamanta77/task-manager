@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.taskmanager.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskmanager.app.data.api.NoteDto

private val noteTextColor = Color(0xFF1A1D26)

fun parseNoteColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFFFFFFFF)
}

@Composable
fun NoteCard(
    note: NoteDto,
    onOpen: () -> Unit,
    checklistProgress: Pair<Int, Int>? = null
) {
    val bg = remember(note.color) { parseNoteColor(note.color) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = noteTextColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = noteTextColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = noteTextColor.copy(alpha = 0.82f),
                    maxLines = 9,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (checklistProgress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Checklist,
                        contentDescription = null,
                        tint = noteTextColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${checklistProgress.first}/${checklistProgress.second} done",
                        style = MaterialTheme.typography.labelMedium,
                        color = noteTextColor.copy(alpha = 0.7f)
                    )
                }
            }
            if (note.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    note.labels.take(3).forEach { label ->
                        NoteLabelPill(label = label)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteLabelPill(label: com.taskmanager.app.data.api.LabelDto) {
    val dot = remember(label.color) {
        label.color?.let { parseNoteColor(it) } ?: noteTextColor
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(dot.copy(alpha = 0.22f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.name,
            style = MaterialTheme.typography.labelSmall,
            color = noteTextColor.copy(alpha = 0.85f)
        )
    }
}
