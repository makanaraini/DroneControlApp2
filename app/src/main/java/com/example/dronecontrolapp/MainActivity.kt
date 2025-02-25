package com.example.dronecontrolapp

import android.os.Bundle
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            DroneControlAppUI(brokerUrl, username, password)
        }
    }
}

@Composable
fun DroneControlAppUI(brokerUrl: String, username: String, password: String) {
    val context = LocalContext.current
    val mqttHandler = remember { MqttHandler(context) }
    var dronePosition by remember { mutableStateOf(GeoPoint(-1.286389, 36.817223)) }
    var battery by remember { mutableIntStateOf(100) }
    var altitude by remember { mutableDoubleStateOf(0.0) }
    var speed by remember { mutableDoubleStateOf(0.0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        mqttHandler.connect(
            brokerUrl = brokerUrl,
            clientId = "android-client",
            username = username,
            password = password,
            onError = { error -> errorMessage = error }
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
            onClick = { mapController?.setCenter(animatedPosition) }, // Use animatedPosition here
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
fun TelemetrySection(battery: Int, altitude: Double, speed: Double, modifier: Modifier = Modifier) {
    val animatedBattery by animateFloatAsState(targetValue = battery.toFloat(), label = "BatteryAnimation")
    val animatedAltitude by animateFloatAsState(targetValue = altitude.toFloat(), label = "AltitudeAnimation")
    val animatedSpeed by animateFloatAsState(targetValue = speed.toFloat(), label = "SpeedAnimation")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.medium)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Altitude
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                Icons.Default.Height,
                contentDescription = "Altitude",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "Altitude",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "${animatedAltitude.toInt()}m",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        // Speed
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                Icons.Default.Speed,
                contentDescription = "Speed",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "Speed",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "${animatedSpeed.toInt()}m/s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        // Battery
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                Icons.Default.BatteryFull,
                contentDescription = "Battery",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "Battery",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "${animatedBattery.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
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
            onClick = { onSave(broker, user, pass) },
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