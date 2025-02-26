import paho.mqtt.client as mqtt
import random
import time
import json
import ssl
import os

# HiveMQ Cloud Broker details
broker = "72fd58bd8ad34bd088141357462a53e5.s1.eu.hivemq.cloud"
port = 8883  # SSL port
# Better to use environment variables for credentials
username = os.getenv("MQTT_USERNAME", "drone-app")
password = os.getenv("MQTT_PASSWORD", "secure-Password012920")

# Topic base for drone data
topic_base = "drone"

# Simulate drone data with real-time values
def generate_drone_data():
    # Generate random latitude and longitude near a base position
    base_lat, base_lng = 48.8566, 2.3522  # Example: Paris coordinates
    lat = base_lat + random.uniform(-0.01, 0.01)
    lng = base_lng + random.uniform(-0.01, 0.01)
    
    data = {
        "drone_id": "drone123",
        "battery": random.randint(0, 100),
        "speed": round(random.uniform(0.0, 50.0), 2),
        "altitude": round(random.uniform(100.0, 1000.0), 2),
        "latitude": round(lat, 6),
        "longitude": round(lng, 6),
        "timestamp": time.time()
    }
    return data

# MQTT callback when connected
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Successfully connected to MQTT broker")
    else:
        print(f"Failed to connect to MQTT broker with result code {rc}")

# Create MQTT client
client = mqtt.Client()
client.on_connect = on_connect

# Enable SSL/TLS with proper verification (more secure)
context = ssl.create_default_context()
client.tls_set_context(context)
# In production, you should validate certificates:
# client.tls_insecure_set(False)  
# For development/testing only:
client.tls_insecure_set(True)

# Set username and password
client.username_pw_set(username, password)

# Connect to the broker
try:
    client.connect(broker, port, 60)
    client.loop_start()
except Exception as e:
    print(f"Failed to connect: {e}")
    exit(1)

try:
    while True:
        # Generate drone telemetry data
        drone_data = generate_drone_data()
        
        # Publish to separate topics that match Android app subscriptions
        client.publish(f"{topic_base}/position", 
                      json.dumps({"latitude": drone_data["latitude"], 
                                 "longitude": drone_data["longitude"]}))
        
        client.publish(f"{topic_base}/battery", str(drone_data["battery"]))
        client.publish(f"{topic_base}/altitude", str(drone_data["altitude"]))
        client.publish(f"{topic_base}/speed", str(drone_data["speed"]))
        
        # Also publish the complete data to the original topic for backwards compatibility
        client.publish(f"{topic_base}/telemetry", json.dumps(drone_data))
        
        print(f"Published data: {drone_data}")
        
        # Wait for 200ms before sending the next data point
        time.sleep(0.2)
except KeyboardInterrupt:
    print("Simulation stopped by user")
except Exception as e:
    print(f"Error in simulation: {e}")
finally:
    client.loop_stop()
    client.disconnect()
    print("Disconnected from MQTT broker")
