package com.taskmanager.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taskmanager.app.ui.theme.Cyan
import com.taskmanager.app.ui.theme.Danger
import com.taskmanager.app.ui.theme.Warning
import com.taskmanager.app.ui.theme.soft
import com.taskmanager.app.ui.util.capitalizeFirst

@Composable
fun PriorityPill(priority: String) {
    val color: Color = when (priority.lowercase()) {
        "high" -> Danger
        "medium" -> Warning
        else -> Cyan
    }
    Surface(
        color = color.soft(0.15f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = capitalizeFirst(priority),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
