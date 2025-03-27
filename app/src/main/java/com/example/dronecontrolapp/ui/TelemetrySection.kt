package com.example.dronecontrolapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dronecontrolapp.AppLogger
import com.example.dronecontrolapp.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EnhancedTelemetrySection(
    battery: Int,
    altitude: Double,
    speed: Double,
    modifier: Modifier = Modifier
) {
    // Debug logging to verify values are received
    LaunchedEffect(key1 = battery, key2 = altitude, key3 = speed) {
        AppLogger.debug("TelemetrySection values - Battery: $battery%, Altitude: ${altitude}m, Speed: ${speed}m/s")
    }

    val animatedBattery by animateFloatAsState(
        targetValue = battery.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "BatteryAnimation"
    )

    val animatedAltitude by animateFloatAsState(
        targetValue = altitude.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "AltitudeAnimation"
    )

    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "SpeedAnimation"
    )

    val batteryColor = when {
        battery > 80 -> EmeraldGreen // Theme green
        battery > 50 -> Success // Light Green
        battery > 20 -> WarningOrange // Amber
        else -> Error // Red for low battery
    }

    var isLowBatteryBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(battery) {
        while (battery < 20) {
            isLowBatteryBlinking = !isLowBatteryBlinking
            delay(500)
        }
    }

    // No box container - just the row of telemetry items
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelemetryItem(
            icon = Icons.Outlined.KeyboardDoubleArrowUp,
            label = "Alt",
            value = "${altitude.toInt()} m",
            color = MaterialTheme.colorScheme.onBackground
        )
        CircularSpeedIndicator(speed = speed.toFloat())
        BatteryStatusIndicator(
            battery = battery,
            color = batteryColor,
            isBlinking = isLowBatteryBlinking
        )
    }
}

@Composable
fun TelemetryItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun CircularSpeedIndicator(speed: Float) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { speed / 50f }, // Normalize to expected range
            color = AerospaceBlue.copy(alpha = 0.7f),
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${speed.toInt()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "m/s",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun BatteryStatusIndicator(battery: Int, color: Color, isBlinking: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isBlinking) Error else color.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$battery%", 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
