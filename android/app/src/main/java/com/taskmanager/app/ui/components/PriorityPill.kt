package com.taskmanager.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taskmanager.app.ui.theme.Error
import com.taskmanager.app.ui.theme.Secondary
import com.taskmanager.app.ui.theme.Tertiary
import com.taskmanager.app.ui.util.capitalizeFirst

@Composable
fun PriorityPill(priority: String) {
    val color = when (priority.lowercase()) {
        "high" -> Error
        "medium" -> Tertiary
        else -> Secondary
    }
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = capitalizeFirst(priority),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
