package com.example.dronecontrolapp

import java.util.UUID

data class LogEntry(
    val message: String,
    val type: LogType,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = UUID.randomUUID().toString()
) 