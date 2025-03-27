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
import com.example.dronecontrolapp.ui.theme.*

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
        colors = CardDefaults.cardColors(containerColor = getNotificationBackground(log.type))
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = log.type.getIcon(),
                contentDescription = null,
                tint = getNotificationIconColor(log.type),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.message, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = log.timestamp.toFormattedTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close, 
                    contentDescription = "Dismiss", 
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
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
            modifier = Modifier.size(8.dp).background(
                color = getConnectionColor(isConnected, isConnecting), 
                shape = MaterialTheme.shapes.small
            )
        )
        if (!isConnected && !isConnecting) {
            IconButton(onClick = onReconnect, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh, 
                    contentDescription = "Reconnect", 
                    tint = MaterialTheme.colorScheme.error
                )
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
            colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.85f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = OffWhite)
                Text(text = "Drone telemetry stream active!", color = OffWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Utility functions - using theme colors
@Composable
private fun getNotificationBackground(type: LogType): Color = when (type) {
    LogType.SUCCESS -> Success.copy(alpha = 0.1f)
    LogType.ERROR -> Error.copy(alpha = 0.1f)
    LogType.WARNING -> Warning.copy(alpha = 0.1f)
    LogType.INFO -> ElectricCyan.copy(alpha = 0.1f)
}

@Composable
private fun getNotificationIconColor(type: LogType): Color = when (type) {
    LogType.SUCCESS -> Success
    LogType.ERROR -> Error 
    LogType.WARNING -> Warning
    LogType.INFO -> ElectricCyan
}

private fun Long.toFormattedTime(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(this))

@Composable
private fun getConnectionColor(isConnected: Boolean, isConnecting: Boolean): Color = when {
    isConnected -> Success
    isConnecting -> WarningOrange
    else -> Error
}
