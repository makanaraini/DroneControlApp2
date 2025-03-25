package com.example.dronecontrolapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dronecontrolapp.AppLogger
import com.example.dronecontrolapp.viewmodel.DroneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsScreen(
    brokerUrl: String,
    username: String,
    password: String,
    onSave: (String, String, String) -> Unit,
    onBack: () -> Unit,
    droneViewModel: DroneViewModel
) {
    var broker by remember { mutableStateOf(brokerUrl) }
    var user by remember { mutableStateOf(username) }
    var pass by remember { mutableStateOf(password) }
    var debugMode by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Debug MQTT Connection",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = debugMode,
            onCheckedChange = { 
                debugMode = it
                if (it) {
                    AppLogger.info("MQTT Debug: URL=${brokerUrl}, Username=${username}, " +
                                   "Client Connected=${droneViewModel.isMqttConnected()}")
                }
            }
        )
    }
} 