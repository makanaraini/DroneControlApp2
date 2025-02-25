import paho.mqtt.client as mqtt
import random
import time
import json
import ssl

# HiveMQ Cloud Broker details (replace with your actual details)
broker = "ssl://72fd58bd8ad34bd088141357462a53e5.s1.eu.hivemq.cloud"  # SSL URL of HiveMQ Cloud Broker
port = 8883  # SSL port for secure connections
username = "drone-app"  # Username
password = "secure-Password012920"  # Password
topic = "drone/telemetry"  # Topic where the data will be published

# Simulate drone data with real-time values
def generate_drone_data():
    data = {
        "drone_id": "drone123",  # Unique drone ID
        "battery": random.randint(0, 100),  # Random battery level (0-100)
        "speed": round(random.uniform(0.0, 50.0), 2),  # Speed in meters per second
        "altitude": round(random.uniform(100.0, 1000.0), 2),  # Altitude in meters
        "timestamp": time.time()  # Timestamp
    }
    return data

# MQTT callback when connected
def on_connect(client, userdata, flags, rc):
    print(f"Connected with result code {rc}")

# Create MQTT client
client = mqtt.Client()

# Set the on_connect callback
client.on_connect = on_connect

# Enable SSL/TLS (for secure connection)
client.tls_set_context(ssl.create_default_context())
client.tls_insecure_set(True)  # Skip SSL certificate validation (optional, only for testing)

# Set username and password
client.username_pw_set(username, password)

# Connect to the broker
client.connect(broker, port, 60)

# Start the MQTT client loop
client.loop_start()

try:
    while True:
        # Generate drone telemetry data
        drone_data = generate_drone_data()
        payload = json.dumps(drone_data)  # Convert data to JSON
        
        # Publish data to the MQTT broker
        client.publish(topic, payload)
        print(f"Published data: {payload}")  # Debug print

        # Wait for 200ms before sending the next data point
        time.sleep(0.2)
except KeyboardInterrupt:
    print("Exiting...")
    client.loop_stop()  # Stop the loop gracefully when interrupted
