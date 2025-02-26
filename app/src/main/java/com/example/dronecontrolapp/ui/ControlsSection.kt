package com.example.dronecontrolapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dronecontrolapp.AppLogger
import com.example.dronecontrolapp.viewmodel.DroneViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EnhancedControlsSection(
    viewModel: DroneViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Animation states
    var isTakeoffPressed by remember { mutableStateOf(false) }
    var isLandPressed by remember { mutableStateOf(false) }
    var isReturnPressed by remember { mutableStateOf(false) }
    
    val takeoffScale by animateFloatAsState(
        targetValue = if (isTakeoffPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), 
        label = "takeoffScale"
    )
    
    val landScale by animateFloatAsState(
        targetValue = if (isLandPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "landScale"
    )
    
    val returnScale by animateFloatAsState(
        targetValue = if (isReturnPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "returnScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Card Title
                Text(
                    text = "FLIGHT CONTROLS",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Takeoff Button
                    ControlButton(
                        icon = Icons.Default.FlightTakeoff,
                        label = "TAKEOFF",
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        scale = takeoffScale,
                        onClick = {
                            isTakeoffPressed = true
                            coroutineScope.launch {
                                viewModel.sendCommand("takeoff")
                                AppLogger.info("User command: Takeoff initiated")
                                delay(200)
                                isTakeoffPressed = false
                            }
                        }
                    )
                    
                    // Land Button
                    ControlButton(
                        icon = Icons.Default.FlightLand,
                        label = "LAND",
                        backgroundColor = MaterialTheme.colorScheme.error,
                        scale = landScale,
                        onClick = {
                            isLandPressed = true
                            coroutineScope.launch {
                                viewModel.sendCommand("land")
                                AppLogger.info("User command: Land initiated")
                                delay(200)
                                isLandPressed = false
                            }
                        }
                    )
                    
                    // Return To Home Button
                    ControlButton(
                        icon = Icons.Default.Home,
                        label = "RTH",
                        backgroundColor = MaterialTheme.colorScheme.tertiary,
                        scale = returnScale,
                        onClick = {
                            isReturnPressed = true
                            coroutineScope.launch {
                                viewModel.sendCommand("return_home")
                                AppLogger.info("User command: Return to home initiated")
                                delay(200)
                                isReturnPressed = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    scale: Float,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .padding(horizontal = 8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
} 