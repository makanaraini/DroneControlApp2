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

# Starting values for simulation
base_lat, base_lng = -1.280010066301841, 36.81598408904595  # Example: JKML coordinates
current_lat, current_lng = base_lat, base_lng
current_battery = 100  # Start with full battery
current_speed = 0.0
target_speed = 15.0  # Target speed in m/s
current_altitude = 100.0
target_altitude = 500.0  # Target altitude in meters
simulation_start_time = time.time()
total_simulation_time = 3600  # 1 hour total flight time

# Simulate drone data with realistic values
def generate_drone_data():
    global current_lat, current_lng, current_battery, current_speed, current_altitude
    global target_speed, target_altitude
    
    # Gradually increment position by 0.001 degrees
    current_lat += 0.0001
    current_lng += 0.0001
    
    # Calculate elapsed time for battery depletion (0-100% over total simulation time)
    elapsed_time = time.time() - simulation_start_time
    battery_drain_percentage = min(100, 100 - (elapsed_time / total_simulation_time * 100))
    current_battery = max(0, int(battery_drain_percentage))
    
    # Gradually adjust speed (accelerate/decelerate smoothly)
    speed_increment = 0.5  # m/s per update
    if current_speed < target_speed:
        current_speed = min(target_speed, current_speed + speed_increment)
    elif current_speed > target_speed:
        current_speed = max(target_speed, current_speed - speed_increment)
    
    # Gradually adjust altitude
    altitude_increment = 5.0  # meters per update
    if current_altitude < target_altitude:
        current_altitude = min(target_altitude, current_altitude + altitude_increment)
    elif current_altitude > target_altitude:
        current_altitude = max(target_altitude, current_altitude - altitude_increment)
    
    # Every 30 seconds, change target speed and altitude to simulate flight maneuvers
    if int(elapsed_time) % 30 == 0 and int(elapsed_time) > 0:
        target_speed = random.uniform(5.0, 25.0)
        target_altitude = random.uniform(100.0, 1000.0)
    
    data = {
        "drone_id": "drone123",
        "battery": current_battery,
        "speed": round(current_speed, 2),
        "altitude": round(current_altitude, 2),
        "latitude": round(current_lat, 6),
        "longitude": round(current_lng, 6),
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
        
        # Wait for 3s before sending the next data point
        time.sleep(3)
except KeyboardInterrupt:
    print("Simulation stopped by user")
except Exception as e:
    print(f"Error in simulation: {e}")
finally:
    client.loop_stop()
    client.disconnect()
    print("Disconnected from MQTT broker")
