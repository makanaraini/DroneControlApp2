package com.example.dronecontrolapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Extension functions for LogType to maintain backward compatibility
 * after refactoring to property-based enum
 */

// Simple pass-through to the icon property
fun LogType.getIcon(): ImageVector = this.icon

// Background colors for different log types in notifications
fun LogType.getBackgroundColor(): Color = when (this) {
    LogType.SUCCESS -> Color(0xFFC8E6C9)  // Light green background
    LogType.ERROR -> Color(0xFFFFCDD2)    // Light red background
    LogType.WARNING -> Color(0xFFFFE0B2)  // Light amber background
    LogType.INFO -> Color(0xFFF8BBD0)     // Light pink background for info
}

// Accent/icon colors for different log types
fun LogType.getIconColor(): Color = when (this) {
    LogType.SUCCESS -> Color(0xFF4CAF50)  // Green
    LogType.ERROR -> Color(0xFFF44336)    // Red
    LogType.WARNING -> Color(0xFFFFC107)  // Amber
    LogType.INFO -> Color(0xFFEC407A)     // Pink
} 