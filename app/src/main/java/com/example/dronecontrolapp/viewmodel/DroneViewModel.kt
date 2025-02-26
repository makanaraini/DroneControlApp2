package com.example.dronecontrolapp.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dronecontrolapp.AppLogger
import com.example.dronecontrolapp.MqttHandler
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.osmdroid.util.GeoPoint

class DroneViewModel(private val context: Context) : ViewModel() {
    private val mqttHandler = MqttHandler(context)
    
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
    
    fun connect() {
        isConnecting.value = true
        connectionStatus.value = "Connecting to ${brokerUrl.value}..."
        errorMessage.value = null
        
        viewModelScope.launch {
            try {
                mqttHandler.connect(
                    brokerUrl = brokerUrl.value,
                    clientId = "android-client-${System.currentTimeMillis()}",
                    username = username.value,
                    password = password.value,
                    onSuccess = { 
                        connectionStatus.value = "Connected successfully to MQTT broker!"
                        AppLogger.info("MQTT connection success callback triggered")
                        subscribeToTopics()
                    },
                    onError = { error -> 
                        errorMessage.value = error
                        connectionStatus.value = "Connection failed: $error"
                        AppLogger.error("MQTT connection error: $error") 
                        isConnecting.value = false
                    }
                )
            } catch (e: Exception) {
                connectionStatus.value = "Exception during connection: ${e.message}"
                AppLogger.error("Exception during MQTT connection", e)
                isConnecting.value = false
            }
        }
    }
    
    private fun subscribeToTopics() {
        AppLogger.info("MQTT connection successful, subscribing to topics")
        
        mqttHandler.subscribe("drone/position") { payload ->
            AppLogger.debug("Received position update: $payload")
            try {
                val jsonObject = JSONObject(payload)
                val latitude = jsonObject.getDouble("latitude")
                val longitude = jsonObject.getDouble("longitude")
                dronePosition.value = GeoPoint(latitude, longitude)
            } catch (e: Exception) {
                AppLogger.error("Error parsing position data: ${e.message}")
            }
        }
        
        mqttHandler.subscribe("drone/battery") { payload ->
            AppLogger.debug("Received battery update: $payload")
            try {
                battery.intValue = payload.toInt()
            } catch (e: Exception) {
                AppLogger.error("Error parsing battery data: ${e.message}")
            }
        }
        
        mqttHandler.subscribe("drone/altitude") { payload ->
            AppLogger.debug("Received altitude update: $payload")
            try {
                altitude.doubleValue = payload.toDouble()
            } catch (e: Exception) {
                AppLogger.error("Error parsing altitude data: ${e.message}")
            }
        }
        
        mqttHandler.subscribe("drone/speed") { payload ->
            AppLogger.debug("Received speed update: $payload")
            try {
                speed.doubleValue = payload.toDouble()
            } catch (e: Exception) {
                AppLogger.error("Error parsing speed data: ${e.message}")
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
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DroneViewModel::class.java)) {
                return DroneViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 