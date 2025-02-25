package com.example.dronecontrolapp

data class DroneData(
    val lat: Double,
    val lon: Double,
    val battery: Int,
    val altitude: Double,
    val speed: Double
)
