package com.taskmanager.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    onOpen: () -> Unit
) {
    val bg = remember(note.color) { parseNoteColor(note.color) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onOpen() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = noteTextColor,
                    modifier = Modifier.weight(1f)
                )
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = noteTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = noteTextColor.copy(alpha = 0.85f),
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
