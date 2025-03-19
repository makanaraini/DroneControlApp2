package com.example.dronecontrolapp

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Import the LogEntry and LogType classes
import com.example.dronecontrolapp.LogEntry
import com.example.dronecontrolapp.LogType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LogManager {
    private val _logs = mutableStateListOf<LogEntry>()
    private val _activeNotifications = mutableStateListOf<LogEntry>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    val logs: List<LogEntry> = _logs
    val activeNotifications: List<LogEntry> = _activeNotifications
    
    fun addLog(message: String, type: LogType) {
        val entry = LogEntry(message, type)
        _logs.add(entry)
        _activeNotifications.add(entry)
        
        // Auto-dismiss notifications after a delay
        coroutineScope.launch {
            // Auto-dismiss after a time period based on type
            val dismissDelay = when(type) {
                LogType.SUCCESS -> 3000L  // Success messages disappear faster
                LogType.INFO -> 5000L     // Info messages stay a bit longer
                else -> 10000L            // Errors/warnings stay longer
            }
            delay(dismissDelay)
            dismissNotification(entry)
        }
        
        // Also log to Android system log
        when (type) {
            LogType.INFO -> AppLogger.info(message)
            LogType.WARNING -> AppLogger.warn(message)
            LogType.ERROR -> AppLogger.error(message)
            LogType.SUCCESS -> AppLogger.info(message)
        }
    }
    
    fun addLogWithoutNotification(message: String, type: LogType) {
        val entry = LogEntry(message, type)
        _logs.add(entry)
        
        // Log to Android system log
        when (type) {
            LogType.INFO -> AppLogger.info(message)
            LogType.WARNING -> AppLogger.warn(message)
            LogType.ERROR -> AppLogger.error(message)
            LogType.SUCCESS -> AppLogger.info(message)
        }
    }
    
    fun dismissNotification(entry: LogEntry) {
        _activeNotifications.remove(entry)
    }
    
    fun clearLogs() {
        _logs.clear()
        _activeNotifications.clear()
    }
} 