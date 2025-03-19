package com.example.dronecontrolapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class LogType(val color: Color, val icon: ImageVector) {
    INFO(Color(0xFFB0BEC5), Icons.Default.Info),         // Grey for info
    WARNING(Color(0xFFFFA000), Icons.Default.Warning),   // Amber for warning
    ERROR(Color(0xFFF44336), Icons.Default.Error),       // Red for error
    SUCCESS(Color(0xFF4CAF50), Icons.Default.CheckCircle); // Green for success
}
