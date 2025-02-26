package com.example.dronecontrolapp

import android.content.Context
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class MqttHandler(context: Context) {
    private var mqttClient: MqttClient? = null

    fun connect(
        brokerUrl: String, 
        clientId: String, 
        username: String, 
        password: String, 
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            AppLogger.debug("Attempting to connect to MQTT broker: $brokerUrl")

            // Ensure we're not already connected
            if (mqttClient?.isConnected == true) {
                AppLogger.debug("Already connected to MQTT broker")
                return
            }

            mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 60  // Increase timeout to 60 seconds
                keepAliveInterval = 30  // Keep at 30 seconds
                isAutomaticReconnect = true  // Enable automatic reconnection
                maxReconnectDelay = 5000  // 5 seconds max reconnect delay
                
                // Only set credentials if provided
                if (username.isNotEmpty()) {
                    this.userName = username
                    this.password = password.toCharArray()
                }

                // Only configure SSL for secure connections
                if (brokerUrl.startsWith("ssl://")) {
                    // Create a trust manager that doesn't validate certificate chains
                    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    })

                    // Install the all-trusting trust manager
                    val sslContext = SSLContext.getInstance("TLSv1.2")
                    sslContext.init(null, trustAllCerts, SecureRandom())
                    
                    // Set the custom socket factory
                    socketFactory = sslContext.socketFactory
                }
            }

            // Set the MQTT callback
            mqttClient?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    AppLogger.info("MQTT connection complete. Reconnect: $reconnect")
                    onSuccess()
                }

                override fun connectionLost(cause: Throwable?) {
                    AppLogger.error("MQTT connection lost: ${cause?.message}")
                    onError("Connection lost: ${cause?.message}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    AppLogger.debug("Message received on topic: $topic - ${message.toString()}")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    AppLogger.debug("Message delivery complete")
                }
            })

            AppLogger.debug("Connecting with options: $options")
            mqttClient?.connect(options)
            AppLogger.info("Connected to MQTT broker successfully")
            onSuccess()

        } catch (e: MqttException) {
            val cause = e.cause
            val causeMessage = cause?.message ?: "No cause details"
            val errorMsg = "Error connecting to MQTT broker (${e.reasonCode}): ${e.message}, Cause: $causeMessage"
            AppLogger.error(errorMsg, e)
            onError(errorMsg)
        } catch (e: Exception) {
            val errorMsg = "Unexpected error connecting to MQTT broker: ${e.message}"
            AppLogger.error(errorMsg, e)
            onError(errorMsg)
        }
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        try {
            mqttClient?.subscribe(topic) { _, message ->
                val payload = String(message.payload)
                Log.d("MqttHandler", "Received message: $payload")
                callback(payload)
            }
        } catch (e: MqttException) {
            Log.e("MqttHandler", "Error subscribing to topic: ${e.message}")
        }
    }

    fun publish(topic: String, message: String) {
        try {
            mqttClient?.publish(topic, message.toByteArray(), 0, false)
            Log.d("MqttHandler", "Published to $topic: $message")
        } catch (e: MqttException) {
            Log.e("MqttHandler", "Error publishing to $topic: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            Log.d("MqttHandler", "Disconnected from MQTT broker")
        } catch (e: MqttException) {
            Log.e("MqttHandler", "Error disconnecting from MQTT broker: ${e.message}")
        }
    }
}
