package com.example.dronecontrolapp

import android.util.Log

object AppLogger {
    private const val TAG = "DroneControlApp"
    private var isLoggingEnabled = true

    fun debug(message: String) {
        if (isLoggingEnabled) Log.d(TAG, message)
    }

    fun info(message: String) {
        if (isLoggingEnabled) Log.i(TAG, message)
    }

    fun warn(message: String) {
        if (isLoggingEnabled) Log.w(TAG, message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (isLoggingEnabled) {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        }
    }

    fun enableLogging(enabled: Boolean) {
        isLoggingEnabled = enabled
    }
} 