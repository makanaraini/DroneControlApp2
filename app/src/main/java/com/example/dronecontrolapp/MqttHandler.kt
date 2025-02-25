package com.example.dronecontrolapp

import android.content.Context
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttHandler(context: Context) {
    private var mqttClient: MqttClient? = null

    fun connect(brokerUrl: String, clientId: String) {
        try {
            mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
            }
            mqttClient?.connect(options)
            Log.d("MqttHandler", "Connected to MQTT broker")
        } catch (e: MqttException) {
            Log.e("MqttHandler", "Error connecting to MQTT broker: ${e.message}")
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

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            Log.d("MqttHandler", "Disconnected from MQTT broker")
        } catch (e: MqttException) {
            Log.e("MqttHandler", "Error disconnecting from MQTT broker: ${e.message}")
        }
    }
}