package com.antigravity.remote.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.remote.data.model.AgentSession
import com.antigravity.remote.data.model.ConnectionState
import com.antigravity.remote.data.model.SessionStatus
import com.antigravity.remote.ui.theme.*
import com.antigravity.remote.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onSessionClick: (String) -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    var showNewSessionDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ConnectionStatusBar(
            connectionState = connectionState,
            serverUrl = serverUrl,
            onConnect = { viewModel.connect() },
            onDisconnect = { viewModel.disconnect() }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Agent Sessions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (connectionState is ConnectionState.Connected) {
                FilledTonalButton(onClick = { showNewSessionDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "New Session")
                    Spacer(Modifier.width(4.dp))
                    Text("New")
                }
            }
        }

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No active sessions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (connectionState !is ConnectionState.Connected) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Connect to a server to see sessions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(session.id) },
                        onStop = { viewModel.stopSession(session.id) }
                    )
                }
            }
        }
    }

    if (showNewSessionDialog) {
        NewSessionDialog(
            onDismiss = { showNewSessionDialog = false },
            onConfirm = { name ->
                viewModel.startNewSession(name)
                showNewSessionDialog = false
            }
        )
    }
}

@Composable
fun ConnectionStatusBar(
    connectionState: ConnectionState,
    serverUrl: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> StatusRunning.copy(alpha = 0.15f)
            is ConnectionState.Connecting -> StatusIdle.copy(alpha = 0.15f)
            is ConnectionState.Error -> StatusError.copy(alpha = 0.15f)
            is ConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "statusBarColor"
    )

    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape),
                    color = when (connectionState) {
                        is ConnectionState.Connected -> StatusRunning
                        is ConnectionState.Connecting -> StatusIdle
                        is ConnectionState.Error -> StatusError
                        is ConnectionState.Disconnected -> StatusCompleted
                    }
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (connectionState) {
                        is ConnectionState.Connected -> "Connected"
                        is ConnectionState.Connecting -> "Connecting..."
                        is ConnectionState.Error -> "Error: ${connectionState.message}"
                        is ConnectionState.Disconnected -> if (serverUrl.isBlank()) "Set server in Settings" else "Disconnected"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            when (connectionState) {
                is ConnectionState.Connected -> {
                    TextButton(onClick = onDisconnect) {
                        Text("Disconnect")
                    }
                }
                is ConnectionState.Disconnected, is ConnectionState.Error -> {
                    if (serverUrl.isNotBlank()) {
                        FilledTonalButton(onClick = onConnect) {
                            Text("Connect")
                        }
                    }
                }
                else -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: AgentSession,
    onClick: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (session.status == SessionStatus.WAITING_FOR_INPUT)
                StatusWaiting.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape),
                        color = when (session.status) {
                            SessionStatus.RUNNING -> StatusRunning
                            SessionStatus.IDLE -> StatusIdle
                            SessionStatus.WAITING_FOR_INPUT -> StatusWaiting
                            SessionStatus.ERROR -> StatusError
                            SessionStatus.COMPLETED -> StatusCompleted
                        }
                    ) {}
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = session.status.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = session.model,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (session.status == SessionStatus.WAITING_FOR_INPUT) {
                Icon(
                    Icons.Filled.NotificationsActive,
                    contentDescription = "Needs input",
                    tint = StatusWaiting
                )
            }
            IconButton(onClick = onStop) {
                Icon(
                    Icons.Filled.Stop,
                    contentDescription = "Stop session",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun NewSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var sessionName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Session") },
        text = {
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("Session Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(sessionName) },
                enabled = sessionName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}