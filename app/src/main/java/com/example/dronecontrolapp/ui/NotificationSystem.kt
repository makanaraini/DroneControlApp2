package com.example.dronecontrolapp.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import com.example.dronecontrolapp.LogEntry
import com.example.dronecontrolapp.LogType
import com.example.dronecontrolapp.getIcon

@Composable
fun NotificationOverlay(
    logs: List<LogEntry>,
    onDismiss: (LogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        logs.takeLast(3).forEach { log ->
            key(log.id) {
                AnimatedNotification(log = log, onDismiss = { onDismiss(log) })
            }
        }
    }
}

@Composable
fun AnimatedNotification(log: LogEntry, onDismiss: () -> Unit) {
    var isVisible by remember { mutableStateOf(true) }
    val dismissDelay = if (log.message.contains("Connected", ignoreCase = true)) 3000L else 5000L

    LaunchedEffect(log) {
        delay(dismissDelay)
        isVisible = false
        delay(300)
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
    ) {
        NotificationCard(log, { isVisible = false })
    }
}

@Composable
private fun NotificationCard(log: LogEntry, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = log.type.getBackgroundColor())
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = log.type.getIcon(),
                contentDescription = null,
                tint = log.type.getIconColor(),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = log.message, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                Text(
                    text = log.timestamp.toFormattedTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun ConnectionIndicator(isConnected: Boolean, isConnecting: Boolean, onReconnect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier.size(8.dp).background(color = getConnectionColor(isConnected, isConnecting), shape = MaterialTheme.shapes.small)
        )
        if (!isConnected && !isConnecting) {
            IconButton(onClick = onReconnect, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reconnect", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ConnectionActiveIndicator(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.85f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                Text(text = "Drone telemetry stream active!", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Utility functions
private fun LogType.getBackgroundColor() = when (this) {
    LogType.SUCCESS -> Color(0xFFC8E6C9)
    LogType.ERROR -> Color(0xFFFFCDD2)
    LogType.WARNING -> Color(0xFFFFE0B2)
    LogType.INFO -> Color(0xFFF8BBD0)
}

private fun LogType.getIconColor() = when (this) {
    LogType.SUCCESS -> Color(0xFF4CAF50)
    LogType.ERROR -> Color(0xFFF44336)
    LogType.WARNING -> Color(0xFFFFC107)
    LogType.INFO -> Color(0xFFEC407A)
}

private fun Long.toFormattedTime(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(this))

private fun getConnectionColor(isConnected: Boolean, isConnecting: Boolean) = when {
    isConnected -> Color(0xFF4CAF50)
    isConnecting -> Color(0xFFFFA000)
    else -> Color(0xFFF44336)
}
