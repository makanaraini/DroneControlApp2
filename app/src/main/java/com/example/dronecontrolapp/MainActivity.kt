package com.example.dronecontrolapp

import android.os.Bundle
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DroneControlAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showMainUI by remember { mutableStateOf(false) }

                    if (showMainUI) {
                        MainActivityUI() // Show the main UI after the splash screen
                    } else {
                        SplashScreen(onLoadingComplete = { showMainUI = true }) // Show the splash screen
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onLoadingComplete: () -> Unit) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animate the logo
        scale.animateTo(1f, animationSpec = tween(1000))
        alpha.animateTo(1f, animationSpec = tween(1000))

        delay(1000) // Additional delay after animation
        onLoadingComplete() // Transition to the main UI
    }

    // Splash screen UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Flight,
            contentDescription = "Drone Logo",
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = alpha.value),
            modifier = Modifier
                .size(100.dp)
                .scale(scale.value)
        )
    }
}

@Composable
fun MainActivityUI() {
    var showSettings by remember { mutableStateOf(false) }
    var brokerUrl by remember { mutableStateOf("ssl://72fd58bd8ad34bd088141357462a53e5.s1.eu.hivemq.cloud:8883") }
    var username by remember { mutableStateOf("drone-app") }
    var password by remember { mutableStateOf("secure-Password012920") }

    // Add logging for screen transitions
    LaunchedEffect(showSettings) {
        AppLogger.debug("Screen changed: ${if (showSettings) "Settings" else "Main"}")
    }

    Crossfade(
        targetState = showSettings,
        label = "ScreenTransition"
    ) { isSettingsVisible ->
        if (isSettingsVisible) {
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
            DroneControlAppUI(
                brokerUrl = brokerUrl, 
                username = username, 
                password = password,
                onNavigateToSettings = { showSettings = true }
            )
        }
    }
}

@Composable
fun DroneControlAppUI(
    brokerUrl: String, 
    username: String, 
    password: String,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val mqttHandler = remember { MqttHandler(context) }
    var dronePosition by remember { mutableStateOf(GeoPoint(-1.286389, 36.817223)) }
    var battery by remember { mutableIntStateOf(100) }
    var altitude by remember { mutableDoubleStateOf(0.0) }
    var speed by remember { mutableDoubleStateOf(0.0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(true) }
    var connectionStatus by remember { mutableStateOf("Initializing...") }

    LaunchedEffect(Unit) {
        AppLogger.info("Attempting MQTT connection to: $brokerUrl")
        connectionStatus = "Connecting to $brokerUrl..."
        
        try {
            mqttHandler.connect(
                brokerUrl = brokerUrl,
                clientId = "android-client-${System.currentTimeMillis()}",
                username = username,
                password = password,
                onSuccess = { 
                    connectionStatus = "Connected successfully to MQTT broker!"
                    AppLogger.info("MQTT connection success callback triggered")
                },
                onError = { error -> 
                    errorMessage = error
                    connectionStatus = "Connection failed: $error"
                    AppLogger.error("MQTT connection error: $error") 
                }
            )
        } catch (e: Exception) {
            connectionStatus = "Exception during connection: ${e.message}"
            AppLogger.error("Exception during MQTT connection", e)
        }
        
        isConnecting = false
        AppLogger.debug("MQTT connection attempt completed")

        if (errorMessage == null) {
            AppLogger.info("MQTT connection successful, subscribing to topics")
            mqttHandler.subscribe("drone/position") { payload ->
                AppLogger.debug("Received position update: $payload")
                try {
                    // Parse JSON position data
                    val jsonObject = org.json.JSONObject(payload)
                    val latitude = jsonObject.getDouble("latitude")
                    val longitude = jsonObject.getDouble("longitude")
                    dronePosition = GeoPoint(latitude, longitude)
                } catch (e: Exception) {
                    AppLogger.error("Error parsing position data: ${e.message}")
                }
            }
            mqttHandler.subscribe("drone/battery") { payload ->
                AppLogger.debug("Received battery update: $payload")
                try {
                    battery = payload.toInt()
                } catch (e: Exception) {
                    AppLogger.error("Error parsing battery data: ${e.message}")
                }
            }
            mqttHandler.subscribe("drone/altitude") { payload ->
                AppLogger.debug("Received altitude update: $payload")
                try {
                    altitude = payload.toDouble()
                } catch (e: Exception) {
                    AppLogger.error("Error parsing altitude data: ${e.message}")
                }
            }
            mqttHandler.subscribe("drone/speed") { payload ->
                AppLogger.debug("Received speed update: $payload")
                try {
                    speed = payload.toDouble()
                } catch (e: Exception) {
                    AppLogger.error("Error parsing speed data: ${e.message}")
                }
            }
        }
    }

    // Show loading indicator
    if (isConnecting) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Add app bar with settings button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Drone Control",
                    style = MaterialTheme.typography.titleLarge
                )
                
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }

            // Display error message if connection fails
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Map Section
            Box(modifier = Modifier.weight(0.85f)) {
                OSMMapView(dronePosition, modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Controls Section
            ControlsSection(mqttHandler, modifier = Modifier.weight(0.07f))

            Spacer(modifier = Modifier.height(4.dp))

            // Telemetry Section
            TelemetrySection(battery, altitude, speed, modifier = Modifier.weight(0.08f))

            // Connection status
            Text(
                text = connectionStatus,
                color = if (connectionStatus.startsWith("Connected")) 
                    MaterialTheme.colorScheme.primary 
                else if (connectionStatus.startsWith("Connection failed") || connectionStatus.startsWith("Exception")) 
                    MaterialTheme.colorScheme.error
                else 
                    MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun OSMMapView(dronePosition: GeoPoint, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mapController by remember { mutableStateOf<org.osmdroid.views.MapController?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var animatedPosition by remember { mutableStateOf(dronePosition) } // Add this line

    LaunchedEffect(dronePosition) {
        // Animate the marker's position
        AppLogger.debug("Drone position updated: ${dronePosition.latitude}, ${dronePosition.longitude}")
        animatedPosition = dronePosition
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    mapController = this.controller as org.osmdroid.views.MapController
                    mapController?.setZoom(15.0)
                    mapController?.setCenter(animatedPosition) // Use animatedPosition here

                    val originalIcon = ContextCompat.getDrawable(ctx, R.drawable.drone_icon)

                    val droneIcon = originalIcon?.let {
                        val width = 50  // Set desired width (in pixels)
                        val height = 50 // Set desired height (in pixels)
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            (it as BitmapDrawable).bitmap, width, height, false
                        )
                        BitmapDrawable(ctx.resources, scaledBitmap)
                    }

                    marker = Marker(this).apply {
                        position = animatedPosition // Use animatedPosition here
                        icon = droneIcon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Drone Location"
                    }
                    this.overlays.add(marker)
                    this.invalidate()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .shadow(4.dp, shape = MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)
        )

        FloatingActionButton(
            onClick = { 
                AppLogger.debug("Map recentered to drone position")
                mapController?.setCenter(animatedPosition) 
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(40.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Recenter", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ControlsSection(mqttHandler: MqttHandler, modifier: Modifier = Modifier) {
    val takeoffScale = remember { Animatable(1f) }
    val landScale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Takeoff Button
        Button(
            onClick = { 
                coroutineScope.launch {
                    takeoffScale.animateTo(0.9f, animationSpec = spring(stiffness = 300f))
                    takeoffScale.animateTo(1f, animationSpec = spring(stiffness = 300f))
                }
                AppLogger.info("User command: Takeoff initiated")
                mqttHandler.publish("drone/commands", "takeoff")
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .scale(takeoffScale.value)
                .height(32.dp),
        ) {
            Icon(Icons.Default.FlightTakeoff, contentDescription = "Takeoff", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Takeoff", style = MaterialTheme.typography.labelSmall)
        }

        // Land Button
        Button(
            onClick = {
                coroutineScope.launch {
                    landScale.animateTo(0.9f, animationSpec = spring(stiffness = 300f))
                    landScale.animateTo(1f, animationSpec = spring(stiffness = 300f))
                }
                AppLogger.info("User command: Land initiated")
                mqttHandler.publish("drone/commands", "land")
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .scale(landScale.value)
                .height(32.dp)
        ) {
            Icon(Icons.Default.FlightLand, contentDescription = "Land", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Land", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun TelemetrySection(
    battery: Int,
    altitude: Double,
    speed: Double,
    modifier: Modifier = Modifier
) {
    // Log significant telemetry changes
    LaunchedEffect(battery, altitude, speed) {
        AppLogger.debug("Telemetry updated - Battery: $battery%, Altitude: ${altitude}m, Speed: ${speed}m/s")
    }

    val animatedBattery by animateFloatAsState(
        targetValue = battery.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "BatteryAnimation"
    )
    val animatedAltitude by animateFloatAsState(
        targetValue = altitude.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "AltitudeAnimation"
    )
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SpeedAnimation"
    )

    // Determine battery icon dynamically
    val batteryIcon = when {
        battery > 80 -> Icons.Default.BatteryFull
        battery > 50 -> Icons.Default.BatteryChargingFull
        battery > 20 -> Icons.Default.BatteryStd
        else -> Icons.Default.BatteryAlert
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Altitude
            TelemetryItem(
                icon = Icons.Outlined.Terrain,
                label = "Altitude",
                value = "${animatedAltitude.toInt()}m",
                modifier = Modifier.weight(1f)
            )

            // Divider
            HorizontalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
            )

            // Speed
            TelemetryItem(
                icon = Icons.Default.Speed,
                label = "Speed",
                value = "${animatedSpeed.toInt()}m/s",
                modifier = Modifier.weight(1f)
            )

            // Divider
            HorizontalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
            )

            // Battery
            TelemetryItem(
                icon = batteryIcon,
                label = "Battery",
                value = "${animatedBattery.toInt()}%",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TelemetryItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1
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
    var broker by remember { mutableStateOf(brokerUrl) }
    var user by remember { mutableStateOf(username) }
    var pass by remember { mutableStateOf(password) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = broker,
            onValueChange = { broker = it },
            label = { Text("Broker URL") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { 
                AppLogger.info("Settings saved - Broker URL: $broker")
                onSave(broker, user, pass) 
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
        DroneControlAppUI(
            brokerUrl = "ssl://72fd58bd8ad34bd088141357462a53e5.s1.eu.hivemq.cloud:8883", 
            username = "drone-app", 
            password = "secure-Password012920",
            onNavigateToSettings = {}  // Add empty function for preview
        )
    }
}