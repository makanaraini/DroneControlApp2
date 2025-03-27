package com.example.dronecontrolapp.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dronecontrolapp.LogEntry
import com.example.dronecontrolapp.LogManager
import com.example.dronecontrolapp.LogType
import java.text.SimpleDateFormat
import java.util.*
import com.example.dronecontrolapp.ui.theme.*

@Composable
fun LogViewerDialog(onDismiss: () -> Unit) {
    var selectedType by remember { mutableStateOf<LogType?>(null) }
    val logs = remember(selectedType) {
        LogManager.logs.let { if (selectedType != null) it.filter { log -> log.type == selectedType } else it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = { DialogTitle(onClearLogs = { LogManager.clearLogs() }) },
        text = { LogContent(logs, selectedType) { selectedType = it } },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DialogTitle(onClearLogs: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("System Logs", fontWeight = FontWeight.Bold)
        IconButton(onClick = onClearLogs) {
            Icon(
                Icons.Default.Delete, 
                contentDescription = "Clear logs", 
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LogContent(logs: List<LogEntry>, selectedType: LogType?, onTypeSelected: (LogType?) -> Unit) {
    Column {
        LogTypeFilter(selectedType, onTypeSelected)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            items(logs.reversed()) { log ->
                LogEntryItem(log)
                Divider(color = Color.LightGray)
            }
        }
    }
}

@Composable
private fun LogTypeFilter(selectedType: LogType?, onTypeSelected: (LogType?) -> Unit) {
    ScrollableRow(modifier = Modifier.fillMaxWidth()) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.padding(end = 8.dp)
        )
        LogType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.name) },
                leadingIcon = { Icon(imageVector = type.icon, contentDescription = null, tint = type.color) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun LogEntryItem(log: LogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = log.type.icon, 
            contentDescription = null, 
            tint = log.type.color, 
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
                text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ScrollableRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier = modifier.horizontalScroll(rememberScrollState()), content = content)
}
