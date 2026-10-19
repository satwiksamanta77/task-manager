package com.taskmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskmanager.app.ui.theme.Background
import com.taskmanager.app.ui.theme.Primary
import com.taskmanager.app.ui.theme.Secondary

@Composable
fun AuthHeader(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Brush.horizontalGradient(listOf(Primary, Secondary))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Background
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Background.copy(alpha = 0.85f)
            )
        }
    }
}
