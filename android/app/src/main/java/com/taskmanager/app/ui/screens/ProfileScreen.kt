package com.taskmanager.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskmanager.app.ui.components.ErrorBanner
import com.taskmanager.app.ui.components.PremiumCard
import com.taskmanager.app.ui.components.PremiumTopBar
import com.taskmanager.app.ui.theme.Bg
import com.taskmanager.app.ui.theme.Danger
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.ui.theme.IrisDeep
import com.taskmanager.app.ui.theme.Text
import com.taskmanager.app.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    onLoggedOut: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = Bg) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PremiumTopBar(title = "Profile")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                if (state.loading && state.user == null) {
                    CircularProgressIndicator(color = Iris)
                } else {
                    val user = state.user
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Iris, IrisDeep))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.name?.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = user?.name?.ifBlank { "TaskManager user" } ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Text
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Dim
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Session",
                                style = MaterialTheme.typography.titleSmall,
                                color = Text
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "You're signed in on this device. Logging out clears your session token.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Dim
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { vm.logout(onLoggedOut) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                                border = BorderStroke(
                                    1.dp,
                                    Danger.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(Icons.Filled.Logout, contentDescription = null)
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Log out", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    state.error?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        ErrorBanner(text = error)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "TaskManager · v1.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = Dim.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
            }
        }
    }
}
