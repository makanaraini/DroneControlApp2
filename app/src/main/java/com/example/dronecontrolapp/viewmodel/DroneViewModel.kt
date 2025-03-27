package com.example.dronecontrolapp.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
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
import com.example.dronecontrolapp.R
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
    val brokerUrl = mutableStateOf("ssl://72fd58bd8ad34bd088141357462a53e5.s1.eu.hivemq.cloud:8883")  // Use SSL for secure connection
    val username = mutableStateOf("drone-app")  // Username
    val password = mutableStateOf("secure-Password012920")  // Password
    
    // Add a flag to track if we've shown the "drone is live" notification
    private var droneDataReceived = false
    private var isDroneLive = false  // Track if the drone is live
    
    fun connect() {
        isConnecting.value = true
        errorMessage.value = null
        
        AppLogger.info("Attempting to connect to MQTT broker: ${brokerUrl.value}")
        
        // Generate a random client ID for each connection attempt
        val clientId = "DroneControlApp-${UUID.randomUUID().toString().substring(0, 8)}"
        
        // Connect to the MQTT broker
        mqttHandler.connect(
            brokerUrl = brokerUrl.value,
            clientId = clientId,
            username = username.value,
            password = password.value,
            onSuccess = {
                connectionStatus.value = "Connected"
                AppLogger.info("Successfully connected to MQTT broker")
                subscribeToTopics()
            },
            onError = { errorMsg ->
                connectionStatus.value = "Error: $errorMsg"
                errorMessage.value = "Failed to connect: $errorMsg"
                isConnecting.value = false
                AppLogger.error("Failed to connect to MQTT broker: $errorMsg")
            }
        )
    }
    
    private fun subscribeToTopics() {
        AppLogger.info("🔊 Subscribing to drone topics...")
        
        // Print debug info about subscription process
        val topicsToSubscribe = listOf(
            "drone/position", 
            "drone/battery", 
            "drone/altitude", 
            "drone/speed", 
            "drone/telemetry"
        )
        
        AppLogger.info("📋 Will subscribe to topics: ${topicsToSubscribe.joinToString()}")
        
        // Subscribe to each topic
        for (topic in topicsToSubscribe) {
            mqttHandler.subscribe(topic) { payload ->
                AppLogger.info("📬 Processing message from topic: $topic")
                when (topic) {
                    "drone/position" -> processPositionUpdate(payload)
                    "drone/battery" -> processBatteryUpdate(payload)
                    "drone/altitude" -> processAltitudeUpdate(payload)
                    "drone/speed" -> processSpeedUpdate(payload)
                    "drone/telemetry" -> processTelemetryUpdate(payload)
                }
            }
        }
        
        isConnecting.value = false
        AppLogger.info("✅ Topic subscription complete")
    }
    
    // Helper methods to process each type of update
    private fun processPositionUpdate(payload: String) {
        AppLogger.debug("🌍 Received position update: $payload")
        try {
            val jsonObject = JSONObject(payload)
            val latitude = jsonObject.getDouble("latitude")
            val longitude = jsonObject.getDouble("longitude")
            dronePosition.value = GeoPoint(latitude, longitude)
            AppLogger.info("📍 Updated drone position: Lat ${latitude}, Lng ${longitude}")
        } catch (e: Exception) {
            AppLogger.error("❌ Error parsing position data: ${e.message}", e)
        }
    }
    
    private fun processBatteryUpdate(payload: String) {
        AppLogger.debug("🔋 Received battery update: $payload")
        try {
            battery.intValue = payload.toInt()
            AppLogger.info("🔋 Updated battery: ${battery.intValue}%")
        } catch (e: Exception) {
            AppLogger.error("❌ Error parsing battery data: ${e.message}", e)
        }
    }
    
    private fun processAltitudeUpdate(payload: String) {
        AppLogger.debug("🏔️ Received altitude update: $payload")
        try {
            altitude.doubleValue = payload.toDouble()
            AppLogger.info("🏔️ Updated altitude: ${altitude.doubleValue}m")
        } catch (e: Exception) {
            AppLogger.error("❌ Error parsing altitude data: ${e.message}", e)
        }
    }
    
    private fun processSpeedUpdate(payload: String) {
        AppLogger.debug("🚀 Received speed update: $payload")
        try {
            speed.doubleValue = payload.toDouble()
            AppLogger.info("🚀 Updated speed: ${speed.doubleValue}m/s")
        } catch (e: Exception) {
            AppLogger.error("❌ Error parsing speed data: ${e.message}", e)
        }
    }
    
    private fun processTelemetryUpdate(payload: String) {
        AppLogger.debug("📊 Received full telemetry update: $payload")
        try {
            val jsonObject = JSONObject(payload)
            
            // Check if this is the first telemetry data
            if (!isDroneLive) {
                isDroneLive = true
                // Notify user that the drone is live
                AppLogger.info("The drone is live.")
                // Show notification to user
                showNotification("Drone Connected", "Your drone is now live and transmitting data.")
                // Also add to LogManager so it appears in logs
                LogManager.addLog("Drone is now live and transmitting telemetry", LogType.SUCCESS)
            }
            
            // Process all telemetry fields from a single message
            if (jsonObject.has("latitude") && jsonObject.has("longitude")) {
                val latitude = jsonObject.getDouble("latitude")
                val longitude = jsonObject.getDouble("longitude")
                dronePosition.value = GeoPoint(latitude, longitude)
                AppLogger.info("📍 Updated position from telemetry: Lat ${latitude}, Lng ${longitude}")
            }
            
            if (jsonObject.has("battery")) {
                val batteryLevel = jsonObject.getInt("battery")
                battery.intValue = batteryLevel
                AppLogger.info("🔋 Updated battery from telemetry: ${battery.intValue}%")
                if (batteryLevel < 20) {  // Assuming 20% is the low battery threshold
                    AppLogger.warn("Battery is low: ${batteryLevel}%")
                    // Trigger a low battery notification
                }
            }
            
            if (jsonObject.has("altitude")) {
                altitude.doubleValue = jsonObject.getDouble("altitude")
                AppLogger.info("🏔️ Updated altitude from telemetry: ${altitude.doubleValue}m")
            }
            
            if (jsonObject.has("speed")) {
                speed.doubleValue = jsonObject.getDouble("speed")
                AppLogger.info("🚀 Updated speed from telemetry: ${speed.doubleValue}m/s")
            }
            
        } catch (e: Exception) {
            AppLogger.error("❌ Error parsing telemetry data: ${e.message}", e)
        }
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
    
    // You can keep the injectTestData method for possible future debug use
    // but we'll make it more clearly a developer function with a comment

    /**
     * FOR DEVELOPMENT/TESTING ONLY
     * Injects test data into the UI to verify display functionality
     * without requiring a live MQTT connection
     */
    // fun injectTestData() {
    //     dronePosition.value = GeoPoint(-1.2800, 36.8159) // Example coordinates
    //     battery.intValue = 75
    //     altitude.doubleValue = 120.5
    //     speed.doubleValue = 15.8
    //     AppLogger.info("Injected test data into UI")
    // }
    
    fun isMqttConnected(): Boolean {
        return mqttHandler.isConnected()
    }
    
    fun diagnoseConnectionIssues() {
        AppLogger.info("🔍 MQTT Connection Diagnostic")
        AppLogger.info("→ Broker URL: ${brokerUrl.value}")
        AppLogger.info("→ Client ID: DroneControlApp-${UUID.randomUUID().toString().substring(0, 8)}")
        AppLogger.info("→ Username: ${username.value}")
        AppLogger.info("→ Password: ${password.value.replace(Regex("."), "*")}")
        AppLogger.info("→ Current connection status: ${connectionStatus.value}")
        AppLogger.info("→ Is MQTT client connected: ${mqttHandler.isConnected()}")
        
        // Try direct connection with detailed logging
        AppLogger.info("→ Attempting diagnostic connection...")
        connect()
    }
    
    fun testSubscriptions(logger: (String) -> Unit) {
        logger("Testing subscription to drone/test...")
        mqttHandler.subscribe("drone/test") { payload ->
            logger("✅ Received message on drone/test: $payload")
        }
        logger("Publishing test message...")
        mqttHandler.publish("drone/test", "Test message ${System.currentTimeMillis()}")
    }
    
    fun getMqttConnectionInfo(): String {
        return mqttHandler.getConnectionInfo()
    }
    
    fun showNotification(title: String, message: String) {
        // For Android 13 and above, we need to check for permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // We'll log a warning - the actual permission request should be handled in the UI layer
            AppLogger.warn("Notification may not show: POST_NOTIFICATIONS permission must be granted on Android 13+")
        }
        
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "drone_notifications"
        
        // Create notification channel if it doesn't exist (for Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Drone Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Create the notification
        val notification = NotificationCompat.Builder(getApplication(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using a system icon as fallback
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Show the notification
        notificationManager.notify(1, notification)
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