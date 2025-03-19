package com.example.dronecontrolapp.ui

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dronecontrolapp.AppLogger
import com.example.dronecontrolapp.viewmodel.DroneViewModel
import kotlinx.coroutines.launch
import com.example.dronecontrolapp.ui.theme.AerospaceBlue
import com.example.dronecontrolapp.ui.theme.DarkGray
import com.example.dronecontrolapp.ui.theme.ElectricCyan
import com.example.dronecontrolapp.ui.theme.WarningOrange
import com.example.dronecontrolapp.ui.theme.EmeraldGreen
import com.example.dronecontrolapp.ui.theme.OffWhite
import com.example.dronecontrolapp.ui.theme.Success
import com.example.dronecontrolapp.SmsSender

@Composable
fun EnhancedControlsSection(
    viewModel: DroneViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isTakingOff by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val buttonScale by animateFloatAsState(
        targetValue = if (isTakingOff) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "Button Scale"
    )

    // Define distinct colors for takeoff and landing - using our theme colors
    val takeoffIconColor = EmeraldGreen  // Green for takeoff
    val landingIconColor = WarningOrange  // Orange for landing
    val rthIconColor = DarkGray    // Cyan for RTH

    // No box container - just the row of control buttons
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Directly apply glass morphism to buttons
        GlassMorphicButton(
            icon = if (isTakingOff) Icons.Default.FlightLand else Icons.Default.FlightTakeoff,
            label = if (isTakingOff) "Landing" else "Takeoff",
            scale = buttonScale,
            onClick = {
                isTakingOff = !isTakingOff
                coroutineScope.launch {
                    val command = if (isTakingOff) "takeoff" else "land"
                    handleCommand(viewModel, command)
                    SmsSender.sendCommand("+1234567890", command) // Replace with the drone's phone number
                }
            },
            backgroundGradient = listOf(AerospaceBlue, ElectricCyan),
            isActive = isTakingOff,
            iconTint = if (isTakingOff) landingIconColor else takeoffIconColor
        )

        GlassMorphicButton(
            icon = Icons.Default.Home,
            label = "RTH",
            scale = buttonScale,
            onClick = { showDialog = true },
            backgroundGradient = listOf(AerospaceBlue, ElectricCyan),
            iconTint = rthIconColor
        )
    }

    if (showDialog) {
        ConfirmReturnHomeDialog(
            onConfirm = {
                coroutineScope.launch { 
                    handleCommand(viewModel, "return_home")
                    SmsSender.sendCommand("+1234567890", "return_home") // Replace with the drone's phone number
                }
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun GlassMorphicButton(
    icon: ImageVector,
    label: String,
    scale: Float,
    onClick: () -> Unit,
    backgroundGradient: List<Color> = listOf(Color(0xFFEC407A), Color(0xFFF48FB1)),
    isActive: Boolean = false,
    iconTint: Color = Color.White
) {
    val pulseAnimation by animateFloatAsState(
        targetValue = if (isActive) 0.85f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse Animation"
    )

    val finalScale = if (isActive) scale * pulseAnimation else scale

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(finalScale)
            .padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                // Glass morphism effects with navy theme
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .background(
                    color = if (isActive) AerospaceBlue.copy(alpha = 0.6f) else AerospaceBlue.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (isActive) ElectricCyan.copy(alpha = 0.7f) else ElectricCyan.copy(alpha = 0.4f),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = iconTint
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.Blue
        )
    }
}

@Composable
private fun ConfirmReturnHomeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Return to Home") },
        text = { Text("Are you sure you want the drone to return to the home location?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Yes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("No") }
        }
    )
}

private suspend fun handleCommand(viewModel: DroneViewModel, command: String) {
    try {
        viewModel.sendCommand(command)
        AppLogger.info("User command: ${command.replaceFirstChar { it.uppercase() }} initiated")
    } catch (e: Exception) {
        AppLogger.error("Error executing command: $command", e)
    }
}
