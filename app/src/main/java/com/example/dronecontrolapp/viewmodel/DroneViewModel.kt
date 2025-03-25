package com.example.dronecontrolapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dronecontrolapp.AppLogger
import com.example.dronecontrolapp.MqttHandler
import com.example.dronecontrolapp.LogManager
import com.example.dronecontrolapp.LogType
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

class DroneViewModel(application: Application) : AndroidViewModel(application) {
    private val mqttHandler = MqttHandler(application)
    
    // Connection state
    val isConnecting = mutableStateOf(true)
    val connectionStatus = mutableStateOf("Initializing...")
    val errorMessage = mutableStateOf<String?>(null)
    
    // Drone state
    val dronePosition = mutableStateOf(GeoPoint(-1.286389, 36.817223))
    val battery = mutableIntStateOf(100)
    val altitude = mutableDoubleStateOf(0.0)
    val speed = mutableDoubleStateOf(0.0)
    
    // Connection config
    val brokerUrl = mutableStateOf("ssl://72fd58bd8ad34bd088141357462a53e5.s1.eu.hivemq.cloud:8883")
    val username = mutableStateOf("drone-app")
    val password = mutableStateOf("secure-Password012920")
    
    // Add a flag to track if we've shown the "drone is live" notification
    private var droneDataReceived = false
    
    fun connect() {
        isConnecting.value = true
        connectionStatus.value = "Connecting to ${brokerUrl.value}..."
        errorMessage.value = null
        
        // Reset drone data received flag
        droneDataReceived = false
        
        // Log connection attempt as a notification
        LogManager.addLog("Connecting to MQTT broker...", LogType.INFO)
        
        viewModelScope.launch {
            try {
                mqttHandler.connect(
                    brokerUrl = brokerUrl.value,
                    clientId = "android-client-${System.currentTimeMillis()}",
                    username = username.value,
                    password = password.value,
                    onSuccess = { 
                        connectionStatus.value = "Connected"
                        AppLogger.info("MQTT connection success callback triggered")
                        subscribeToTopics()
                    },
                    onError = { error -> 
                        connectionStatus.value = "Connection failed"
                        LogManager.addLog("Connection failed: $error", LogType.ERROR)
                        AppLogger.error("MQTT connection error: $error") 
                        isConnecting.value = false
                    }
                )
            } catch (e: Exception) {
                connectionStatus.value = "Exception during connection: ${e.message}"
                LogManager.addLog("Connection error: ${e.message}", LogType.ERROR)
                AppLogger.error("Exception during MQTT connection", e)
                isConnecting.value = false
            }
        }
    }
    
    private fun subscribeToTopics() {
        AppLogger.info("MQTT connection successful, subscribing to topics")
        
        // Show initial connection success notification
        LogManager.addLog("Connected to MQTT broker successfully", LogType.SUCCESS)
        
        mqttHandler.subscribe("drone/position") { payload ->
            AppLogger.debug("Received position update: $payload")
            try {
                val jsonObject = JSONObject(payload)
                val latitude = jsonObject.getDouble("latitude")
                val longitude = jsonObject.getDouble("longitude")
                dronePosition.value = GeoPoint(latitude, longitude)
                
                // Log the data in debug logs but don't send a notification
                val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                AppLogger.info("GPS data received ($timestamp): Lat: $latitude, Lng: $longitude")
                
                // Show "Drone is live" notification only on first data received
                if (!droneDataReceived) {
                    droneDataReceived = true
                    LogManager.addLog("Drone telemetry stream active! Drone is now transmitting data.", LogType.SUCCESS)
                }
            } catch (e: Exception) {
                AppLogger.error("Error parsing position data: ${e.message}")
            }
        }
        
        mqttHandler.subscribe("drone/battery") { payload ->
            AppLogger.debug("Received battery update: $payload")
            try {
                battery.intValue = payload.toInt()
                
                // Only log to debug, don't notify
                val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                AppLogger.info("Battery data received ($timestamp): ${battery.intValue}%")
            } catch (e: Exception) {
                AppLogger.error("Error parsing battery data: ${e.message}")
            }
        }
        
        mqttHandler.subscribe("drone/altitude") { payload ->
            AppLogger.debug("Received altitude update: $payload")
            try {
                altitude.doubleValue = payload.toDouble()
                
                // Only log to debug, don't notify
                val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                AppLogger.info("Altitude data received ($timestamp): ${altitude.doubleValue}m")
            } catch (e: Exception) {
                AppLogger.error("Error parsing altitude data: ${e.message}")
            }
        }
        
        mqttHandler.subscribe("drone/speed") { payload ->
            AppLogger.debug("Received speed update: $payload")
            try {
                speed.doubleValue = payload.toDouble()
                
                // Only log to debug, don't notify
                val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                AppLogger.info("Speed data received ($timestamp): ${speed.doubleValue}m/s")
            } catch (e: Exception) {
                AppLogger.error("Error parsing speed data: ${e.message}")
            }
        }
        
        mqttHandler.subscribe("drone/telemetry") { payload ->
            AppLogger.debug("Received complete telemetry update: $payload")
            try {
                val jsonObject = JSONObject(payload)
                // Process complete telemetry data if needed
            } catch (e: Exception) {
                AppLogger.error("Error parsing telemetry data: ${e.message}")
            }
        }
        
        isConnecting.value = false
    }
    
    fun sendCommand(command: String) {
        AppLogger.info("User command: $command initiated")
        mqttHandler.publish("drone/commands", command)
    }
    
    fun updateConnectionSettings(newBroker: String, newUsername: String, newPassword: String) {
        brokerUrl.value = newBroker
        username.value = newUsername
        password.value = newPassword
        connect()
    }
    
    override fun onCleared() {
        super.onCleared()
        mqttHandler.disconnect()
    }
    
    fun updateTelemetryFromSms(telemetry: String) {
        // Parse the telemetry string and update the LiveData properties
        try {
            val jsonObject = JSONObject(telemetry)
            val latitude = jsonObject.getDouble("latitude")
            val longitude = jsonObject.getDouble("longitude")
            dronePosition.value = GeoPoint(latitude, longitude)
            // Update other telemetry data similarly
        } catch (e: Exception) {
            AppLogger.error("Error parsing SMS telemetry data: ${e.message}")
        }
    }

    fun updateTelemetryFromMqtt(telemetry: String) {
        // Similar to SMS, parse the telemetry data and update LiveData
        try {
            val jsonObject = JSONObject(telemetry)
            val latitude = jsonObject.getDouble("latitude")
            val longitude = jsonObject.getDouble("longitude")
            dronePosition.value = GeoPoint(latitude, longitude)
            // Update other telemetry data similarly
        } catch (e: Exception) {
            AppLogger.error("Error parsing MQTT telemetry data: ${e.message}")
        }
    }
    
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DroneViewModel::class.java)) {
                return DroneViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 