package com.example.dronecontrolapp

import android.content.Context
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.net.ssl.SSLSocketFactory

class MqttHandler(context: Context) {
    private var mqttClient: MqttClient? = null

    fun connect(brokerUrl: String, clientId: String, username: String, password: String, onError: (String) -> Unit) {
        try {
            mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
                this.userName = username
                this.password = password.toCharArray()
                socketFactory = SSLSocketFactory.getDefault()
            }
            mqttClient?.connect(options)
            Log.d("MqttHandler", "Connected to MQTT broker")
        } catch (e: MqttException) {
            val errorMsg = "Error connecting to MQTT broker: ${e.message}"
            Log.e("MqttHandler", errorMsg)
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