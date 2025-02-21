# Solar-Powered Low-Cost Cargo Delivery Drone Mobile Control App

![Drone App Screenshot](/sreenshot.jpg)

## Overview
This project is a mobile control app for a **solar-powered, low-cost cargo delivery drone**. The app allows users to control the drone, monitor its telemetry data, and manage cargo delivery tasks via GSM communication. The drone is powered by an **Arduino Mega 2560** and uses a **GSM module** for communication.

## Features
- **Real-Time Drone Control**: Send commands (e.g., takeoff, land, move) to the drone
- **Telemetry Monitoring**: View real-time data such as altitude, speed, and battery level
- **Cargo Management**: Manage cargo delivery tasks and track drone location
- **GSM Communication**: Communicate with the drone via SMS or TCP/IP
- **Map View**: Displays the current location of the drone using OpenStreetMap
- **Solar-Powered**: Designed for sustainability with solar energy integration

## Getting Started

### Prerequisites
- **Hardware**:
  - Arduino Mega 2560
  - GSM module (e.g., SIM800, SIM900)
  - GPS module, sensors, and motors for the drone
  - Solar panel and power management system
- **Software**:
  - Android Studio
  - Arduino IDE
  - SIM card with SMS/data plan
  - Kotlin
  - An Android device or emulator

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/makanaraini/DroneControlApp2.git
   ```

2. **Open the project in Android Studio**:
   - Launch Android Studio
   - Select `Open an existing Android Studio project`
   - Navigate to the cloned repository and select it

3. **Build and Run**:
   - Connect your Android device or start an emulator
   - Click on the `Run` button in Android Studio

4. **Set Up the GSM Module**:
   - Insert a SIM card into the GSM module
   - Connect the GSM module to the Arduino as follows:
     - GSM TX -> Arduino RX (Pin 10)
     - GSM RX -> Arduino TX (Pin 11)
     - GSM GND -> Arduino GND
     - GSM VCC -> External Power Supply (3.7V–4.2V)
   - Power on the GSM module and ensure it has a signal

## Usage

- **Map View**: The map view will automatically center on the drone's current location
- **Takeoff and Land**: Use the buttons to control the drone's takeoff and landing
- **Telemetry**: Monitor the drone's altitude, speed, and battery status in real-time
- **Cargo Management**: Add, edit, or delete cargo delivery tasks via the interface


## Contributing

Contributions are welcome! Please fork the repository and submit a pull request for any improvements or bug fixes.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [osmdroid](https://github.com/osmdroid/osmdroid) for the map view implementation
- Icons from [Material Icons](https://material.io/resources/icons/)
- Thanks to the Arduino and Android communities for their support