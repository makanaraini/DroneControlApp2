package com.example.dronecontrolapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dronecontrolapp.ui.theme.DroneControlAppTheme
import com.example.dronecontrolapp.ui.theme.ElectricCyan
import com.example.dronecontrolapp.viewmodel.DroneViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val serverUrl = "http://your-server-ip:3000" // Change this to your server IP
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DroneControlAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        FuturisticSplashScreen(onLoadingComplete = { showSplash = false })
                    } else {
                        MainApp()
                    }
                }
            }
        }

        startTelemetryUpdates()
    }

    private fun sendControlCommand(command: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("command", command)
                }

                val request = Request.Builder()
                    .url("$serverUrl/control")
                    .post(json.toString().toRequestBody(JSON))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            // Update UI with error
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Update UI with error
                }
            }
        }
    }

    private fun startTelemetryUpdates() {
        scope.launch {
            while (true) {
                val telemetry = JSONObject().apply {
                    put("speed", getDroneSpeed())
                    put("battery", getDroneBattery())
                    put("altitude", getDroneAltitude())
                    put("latitude", getDroneLatitude())
                    put("longitude", getDroneLongitude())
                }

                sendTelemetryData(telemetry)
                delay(1000)
            }
        }
    }

    private fun sendTelemetryData(telemetry: JSONObject) {
        scope.launch {
            try {
                val request = Request.Builder()
                    .url("$serverUrl/telemetry")
                    .post(telemetry.toString().toRequestBody(JSON))
                    .build()

                client.newCall(request).execute()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Placeholder functions - replace with actual implementations
    private fun getDroneSpeed(): Float = 5.0f
    private fun getDroneBattery(): Int = 80
    private fun getDroneAltitude(): Float = 10.0f
    private fun getDroneLatitude(): Double = 37.7749
    private fun getDroneLongitude(): Double = -122.4194

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

@Composable
fun MainApp() {
    val droneViewModel: DroneViewModel = viewModel(
        factory = DroneViewModel.Factory(LocalContext.current)
    )

    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(droneViewModel.isConnecting.value) {
        if (!droneViewModel.isConnecting.value && droneViewModel.connectionStatus.value != "Connected") {
            droneViewModel.connect()
        }
    }

    Crossfade(
        targetState = showSettings,
        label = "ScreenTransition"
    ) { isSettingsVisible ->
        if (isSettingsVisible) {
            EnhancedSettingsScreen(
                brokerUrl = droneViewModel.brokerUrl.value,
                username = droneViewModel.username.value,
                password = droneViewModel.password.value,
                onSave = { newBroker, newUser, newPass ->
                    droneViewModel.updateConnectionSettings(newBroker, newUser, newPass)
                    showSettings = false
                },
                onBack = { showSettings = false }
            )
        } else {
            DroneControlScreen(
                droneViewModel = droneViewModel,
                onNavigateToSettings = { showSettings = true }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneControlScreen(
    droneViewModel: DroneViewModel,
    onNavigateToSettings: () -> Unit
) {
    var showLogViewer by remember { mutableStateOf(false) }
    val isConnecting by droneViewModel.isConnecting
    val errorMessage by droneViewModel.errorMessage
    val dronePosition by droneViewModel.dronePosition
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        EnhancedMapView(
            dronePosition = dronePosition,
            modifier = Modifier.fillMaxSize(),
            keepCentered = true
        )

        EnhancedTelemetrySection(
            battery = droneViewModel.battery.intValue,
            altitude = droneViewModel.altitude.doubleValue,
            speed = droneViewModel.speed.doubleValue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            EnhancedControlsSection(
                viewModel = droneViewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = ElectricCyan
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        showLogViewer = true
                        expanded = false
                    },
                    text = { Text("View Logs") }
                )
                DropdownMenuItem(
                    onClick = {
                        onNavigateToSettings()
                        expanded = false
                    },
                    text = { Text("Settings") }
                )
            }
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .background(
                        Color.White.copy(alpha = 0.8f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            )
        }

        if (showLogViewer) {
            // Implement LogViewerDialog here
        }
    }
}

@Composable
fun EnhancedMapView(
    dronePosition: GeoPoint,
    modifier: Modifier = Modifier,
    keepCentered: Boolean = true
) {
    val context = LocalContext.current
    var mapController by remember { mutableStateOf<org.osmdroid.views.MapController?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(dronePosition) {
        marker?.apply {
            position = dronePosition
            if (keepCentered) {
                mapController?.animateTo(dronePosition)
            }
            mapView?.invalidate()
        }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                mapView = this

                mapController = this.controller as org.osmdroid.views.MapController
                mapController?.setZoom(15.0)
                mapController?.setCenter(dronePosition)

                val originalIcon = ContextCompat.getDrawable(ctx, R.drawable.drone_icon)
                val droneIcon = originalIcon?.let {
                    val width = 50
                    val height = 50
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        (it as BitmapDrawable).bitmap, width, height, false
                    )
                    BitmapDrawable(ctx.resources, scaledBitmap)
                }

                marker = Marker(this).apply {
                    position = dronePosition
                    icon = droneIcon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Drone Location"
                    snippet = "Lat: ${dronePosition.latitude}\nLng: ${dronePosition.longitude}"
                }
                this.overlays.add(marker)
                this.invalidate()
            }
        },
        update = { view -> view.invalidate() },
        modifier = modifier
    )
}

@Composable
fun EnhancedTelemetrySection(
    battery: Int,
    altitude: Double,
    speed: Double,
    modifier: Modifier = Modifier
) {
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

    val batteryIcon = when {
        battery > 80 -> Icons.Default.BatteryFull
        battery > 50 -> Icons.Default.BatteryChargingFull
        battery > 20 -> Icons.Default.BatteryStd
        else -> Icons.Default.BatteryAlert
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TelemetryItem(
                icon = Icons.Outlined.Terrain,
                label = "Altitude",
                value = "${animatedAltitude.toInt()}m",
                modifier = Modifier.weight(1f)
            )

            HorizontalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
            )

            TelemetryItem(
                icon = Icons.Default.Speed,
                label = "Speed",
                value = "${animatedSpeed.toInt()}m/s",
                modifier = Modifier.weight(1f)
            )

            HorizontalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
            )

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
fun EnhancedControlsSection(
    viewModel: DroneViewModel,
    modifier: Modifier = Modifier
) {
    val takeoffScale = remember { Animatable(1f) }
    val landScale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                coroutineScope.launch {
                    takeoffScale.animateTo(0.9f, animationSpec = spring(stiffness = 300f))
                    takeoffScale.animateTo(1f, animationSpec = spring(stiffness = 300f))
                }
                viewModel.sendCommand("takeoff")
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

        Button(
            onClick = {
                coroutineScope.launch {
                    landScale.animateTo(0.9f, animationSpec = spring(stiffness = 300f))
                    landScale.animateTo(1f, animationSpec = spring(stiffness = 300f))
                }
                viewModel.sendCommand("land")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsScreen(
    brokerUrl: String,
    username: String,
    password: String,
    onSave: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var broker by remember { mutableStateOf(brokerUrl) }
    var user by remember { mutableStateOf(username) }
    var pass by remember { mutableStateOf(password) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection Settings") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "MQTT CONNECTION",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = broker,
                        onValueChange = { broker = it },
                        label = { Text("Broker URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Cloud, contentDescription = null)
                        }
                    )

                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("Username") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        }
                    )

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onSave(broker, user, pass) },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE SETTINGS")
            }
        }
    }
}

@Composable
fun FuturisticSplashScreen(onLoadingComplete: () -> Unit) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(1000))
        alpha.animateTo(1f, animationSpec = tween(1000))
        delay(1000)
        onLoadingComplete()
    }

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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DroneControlAppTheme {
        DroneControlScreen(
            droneViewModel = DroneViewModel(LocalContext.current),
            onNavigateToSettings = {}
        )
    }
}