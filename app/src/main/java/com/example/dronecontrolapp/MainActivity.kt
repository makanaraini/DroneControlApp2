package com.example.dronecontrolapp

import android.os.Bundle
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Alignment
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.example.dronecontrolapp.ui.theme.DroneControlAppTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid configuration before setting content
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )

        setContent {
            DroneControlAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSettings by remember { mutableStateOf(false) }

                    // Settings state variables
                    var brokerUrl by remember { mutableStateOf("ssl://72fd58bd8ad34bd088141357462a53e5.s1.eu.hivemq.cloud:8883") }
                    var username by remember { mutableStateOf("drone-app") }
                    var password by remember { mutableStateOf("secure-Password012920") }

                    if (showSettings) {
                        // Show Settings Screen
                        SettingsScreen(
                            brokerUrl = brokerUrl,
                            username = username,
                            password = password,
                            onSave = { newBroker, newUser, newPass ->
                                brokerUrl = newBroker
                                username = newUser
                                password = newPass
                                showSettings = false
                            }
                        )
                    } else {
                        // Main App UI
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Toggle Settings Button
                            IconButton(
                                onClick = { showSettings = true },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Drone Control App UI
                            DroneControlAppUI(
                                brokerUrl = brokerUrl,
                                username = username,
                                password = password
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DroneControlAppUI(
    brokerUrl: String,
    username: String,
    password: String
) {
    val context = LocalContext.current
    val mqttHandler = remember { MqttHandler(context) }

    // State variables
    var dronePosition by remember { mutableStateOf(GeoPoint(-1.286389, 36.817223)) }
    var battery by remember { mutableStateOf(100) }
    var altitude by remember { mutableStateOf(0.0) }
    var speed by remember { mutableStateOf(0.0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(true) }

    // MQTT connection and subscription logic
    LaunchedEffect(Unit) {
        try {
            mqttHandler.connect(
                brokerUrl = brokerUrl,
                clientId = "android-client",
                username = username,
                password = password,
                onError = { error ->
                    errorMessage = error
                }
            )
            isConnecting = false // Connection attempt completed

            if (errorMessage == null) {
                mqttHandler.subscribe("drone/position") { payload ->
                    val (lat, lon) = payload.split(",").map { it.toDouble() }
                    dronePosition = GeoPoint(lat, lon)
                }
                mqttHandler.subscribe("drone/battery") { payload ->
                    battery = payload.toInt()
                }
                mqttHandler.subscribe("drone/altitude") { payload ->
                    altitude = payload.toDouble()
                }
                mqttHandler.subscribe("drone/speed") { payload ->
                    speed = payload.toDouble()
                }
            }
        } catch (e: Exception) {
            errorMessage = "Connection failed: ${e.message}"
            isConnecting = false
        }
    }

    // UI Composition
    if (isConnecting) {
        // Show loading indicator while connecting
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else if (errorMessage != null) {
        // Display error message if connection fails
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    errorMessage = null
                    isConnecting = true // Retry connection
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Retry Connection")
            }
        }
    } else {
        // Main UI when connected successfully
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Map Section
            Box(modifier = Modifier.weight(0.85f)) {
                OSMMapView(dronePosition, modifier = Modifier.fillMaxSize())
            }

            // Controls Section
            ControlsSection(mqttHandler, modifier = Modifier.weight(0.07f))

            // Telemetry Section
            TelemetrySection(battery, altitude, speed, modifier = Modifier.weight(0.08f))
        }
    }
}

@Composable
fun OSMMapView(
    dronePosition: GeoPoint,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapController by remember { mutableStateOf<org.osmdroid.views.MapController?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }

    // Load OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    Box(modifier = modifier) {
        // AndroidView for the OSM MapView
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    // Initialize map controller
                    mapController = this.controller as org.osmdroid.views.MapController
                    mapController?.setZoom(15.0)
                    mapController?.setCenter(dronePosition)

                    // Add drone marker
                    marker = Marker(this).apply {
                        position = dronePosition
                        icon = createScaledDrawable(ctx, R.drawable.drone_icon, 50, 50)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Drone Location"
                    }
                    overlays.add(marker)
                    invalidate()
                }
            },
            update = { mapView ->
                // Update the map and marker when the drone position changes
                mapController?.setCenter(dronePosition)
                marker?.position = dronePosition
                mapView.invalidate()
            },
            modifier = Modifier
                .fillMaxSize()
                .shadow(4.dp, shape = MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)
        )

        // FloatingActionButton to recenter the map
        FloatingActionButton(
            onClick = { mapController?.setCenter(dronePosition) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(40.dp),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "Recenter",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Helper function to scale drawable resources
private fun createScaledDrawable(context: Context, @DrawableRes resId: Int, widthPx: Int, heightPx: Int): Drawable? {
    return ContextCompat.getDrawable(context, resId)?.let { drawable ->
        val bitmap = (drawable as BitmapDrawable).bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, widthPx, heightPx, false)
        BitmapDrawable(context.resources, scaledBitmap)
    }
}

@Composable
fun ControlsSection(mqttHandler: MqttHandler, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reusable composable function for animated buttons
        fun AnimatedButton(
            label: String,
            icon: ImageVector,
            contentDescription: String,
            command: String,
            buttonColor: Color,
            scale: Animatable
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        scale.animateTo(0.9f, animationSpec = spring())
                        scale.animateTo(1f, animationSpec = spring())
                    }
                    mqttHandler.publish("drone/commands", command)
                },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier
                    .scale(scale.value)
                    .height(32.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Takeoff Button
        val takeoffScale = remember { Animatable(1f) }
        AnimatedButton(
            label = "Takeoff",
            icon = Icons.Default.FlightTakeoff,
            contentDescription = "Takeoff",
            command = "takeoff",
            buttonColor = MaterialTheme.colorScheme.primary,
            scale = takeoffScale
        )

        // Land Button
        val landScale = remember { Animatable(1f) }
        AnimatedButton(
            label = "Land",
            icon = Icons.Default.FlightLand,
            contentDescription = "Land",
            command = "land",
            buttonColor = MaterialTheme.colorScheme.error,
            scale = landScale
        )
    }
}

@Composable
fun TelemetrySection(battery: Int, altitude: Double, speed: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .horizontalScroll(rememberScrollState())
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reusable composable function for telemetry items
        fun TelemetryItem(icon: ImageVector, label: String, value: String) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Altitude
        TelemetryItem(
            icon = Icons.Default.Height,
            label = "Altitude",
            value = "${altitude}m"
        )

        // Speed
        TelemetryItem(
            icon = Icons.Default.Speed,
            label = "Speed",
            value = "${speed}m/s"
        )

        // Battery
        TelemetryItem(
            icon = Icons.Default.BatteryFull,
            label = "Battery",
            value = "$battery%"
        )
    }
}

@Composable
fun SettingsScreen(
    brokerUrl: String,
    username: String,
    password: String,
    onSave: (String, String, String) -> Unit
) {
    var broker by rememberSaveable { mutableStateOf(brokerUrl) }
    var user by rememberSaveable { mutableStateOf(username) }
    var pass by rememberSaveable { mutableStateOf(password) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = broker,
            onValueChange = { newValue ->
                if (newValue.length <= 256) {
                    broker = newValue
                }
            },
            label = { Text("Broker URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = user,
            onValueChange = { newValue ->
                if (newValue.length <= 64) {
                    user = newValue
                }
            },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { newValue ->
                if (newValue.length <= 64) {
                    pass = newValue
                }
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (broker.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                    onSave(broker, user, pass)
                } else {
                    Toast.makeText(LocalContext.current, "All fields are required", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DroneControlAppTheme {
        DroneControlAppUI("ssl://72fd58bd8ad34bd088141357462a53e5.s1.eu.hivemq.cloud:8883", "drone-app", "secure-Password012920")
    }
}