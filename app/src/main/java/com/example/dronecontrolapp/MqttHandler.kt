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
            AppLogger.info("🔌 MQTT Connect attempt: Broker=$brokerUrl, ClientID=$clientId")
            
            // Check if already connected
            if (mqttClient?.isConnected == true) {
                AppLogger.info("Already connected to MQTT broker")
                onSuccess()
                AppLogger.info("✅ MQTT connection SUCCESS! Client is now connected: ${mqttClient?.isConnected}")
                return
            }
            
            // Ensure the broker URL is prefixed with "ssl://"
            val processedBrokerUrl = if (brokerUrl.startsWith("ssl://")) {
                brokerUrl
            } else {
                "ssl://$brokerUrl"
            }
            
            AppLogger.debug("Using processed broker URL: $processedBrokerUrl")
            
            mqttClient = MqttClient(processedBrokerUrl, clientId, MemoryPersistence())
            
            // Connection options
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
                isAutomaticReconnect = true
                if (username.isNotEmpty()) {
                    this.userName = username
                    this.password = password.toCharArray()
                    AppLogger.debug("Set MQTT credentials for user: $username")
                }
                
                // SSL/TLS Configuration
                if (processedBrokerUrl.startsWith("ssl://")) {
                    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    })
                    
                    try {
                        val sslContext = SSLContext.getInstance("TLSv1.2")
                        sslContext.init(null, trustAllCerts, SecureRandom())
                        socketFactory = sslContext.socketFactory
                        AppLogger.debug("SSL context configured successfully")
                    } catch (e: Exception) {
                        AppLogger.error("SSL configuration error", e)
                        onError("SSL configuration failed: ${e.message}")
                        return
                    }
                }
            }
            
            AppLogger.debug("MQTT connect options configured, attempting connection...")
            
            mqttClient?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    AppLogger.info("🎉 MQTT connectComplete callback: reconnect=$reconnect, URI=$serverURI")
                    onSuccess()
                    AppLogger.info("✅ MQTT connection SUCCESS! Client is now connected: ${mqttClient?.isConnected}")
                }
                
                override fun connectionLost(cause: Throwable?) {
                    AppLogger.error("❌ MQTT connectionLost callback: ${cause?.message}", cause)
                    onError("Connection lost: ${cause?.message}")
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    AppLogger.debug("Message arrived on topic: $topic, length: ${message?.payload?.size}")
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    AppLogger.debug("Message delivery complete for token: ${token?.messageId}")
                }
            })
            
            // Add connection timeout handler
            val connectionTimeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (mqttClient?.isConnected != true) {
                    AppLogger.error("MQTT connection timed out")
                    onError("Connection timed out after 30 seconds")
                }
            }
            
            try {
                connectionTimeoutHandler.postDelayed(timeoutRunnable, 30000) // 30 second timeout
                AppLogger.debug("Connecting to MQTT broker...")
                mqttClient?.connect(options)
                connectionTimeoutHandler.removeCallbacks(timeoutRunnable) // Clear timeout if connected
                
                AppLogger.info("MQTT connect() completed successfully")
                onSuccess()
            } catch (e: MqttException) {
                connectionTimeoutHandler.removeCallbacks(timeoutRunnable) // Clear timeout on error
                
                // Convert the reasonCode to string for simpler error handling
                val errorMessage = when (e.reasonCode.toInt()) {
                    MqttException.REASON_CODE_BROKER_UNAVAILABLE.toInt() -> "Broker unavailable"
                    MqttException.REASON_CODE_CLIENT_EXCEPTION.toInt() -> "Client exception"
                    MqttException.REASON_CODE_CONNECTION_LOST.toInt() -> "Connection lost"
                    MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt() -> "Client timeout"
                    MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION.toInt() -> "Invalid protocol version"
                    MqttException.REASON_CODE_INVALID_CLIENT_ID.toInt() -> "Invalid client ID"
                    MqttException.REASON_CODE_FAILED_AUTHENTICATION.toInt() -> "Authentication failed - check username/password"
                    MqttException.REASON_CODE_NOT_AUTHORIZED.toInt() -> "Not authorized"
                    else -> "Connection error (code: ${e.reasonCode})"
                }
                
                AppLogger.error("MQTT connection error: $errorMessage", e)
                onError("$errorMessage: ${e.message}")
            }
            
        } catch (e: Exception) {
            AppLogger.error("Unexpected MQTT connection error", e)
            onError("Unexpected error: ${e.message}")
        }
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        try {
            AppLogger.info("🔔 Subscribing to topic: $topic")
            
            // Define a message listener
            val messageListener = IMqttMessageListener { _, message ->
                val payloadText = String(message.payload)
                AppLogger.info("📩 Received on topic $topic: $payloadText")
                callback(payloadText)
            }
            
            // Subscribe using the proper method
            mqttClient?.subscribe(topic, 0, messageListener)
            AppLogger.info("✅ Successfully subscribed to topic: $topic")
        } catch (e: MqttException) {
            AppLogger.error("❌ Error subscribing to topic $topic: ${e.message}", e)
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

    fun isConnected(): Boolean {
        val connected = mqttClient?.isConnected ?: false
        AppLogger.info("MQTT Connection status check: $connected")
        return connected
    }

    fun getConnectionInfo(): String {
        val isConnected = mqttClient?.isConnected ?: false
        val clientId = mqttClient?.clientId ?: "No client ID"
        val serverURI = mqttClient?.serverURI ?: "No server URI"
        
        return "MQTT Connection Info:\n" +
               "Connected: $isConnected\n" +
               "Client ID: $clientId\n" +
               "Server URI: $serverURI"
    }
}
