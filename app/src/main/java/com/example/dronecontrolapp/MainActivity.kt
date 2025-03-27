package com.example.dronecontrolapp

import android.os.Bundle
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dronecontrolapp.ui.*
import com.example.dronecontrolapp.viewmodel.DroneViewModel
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.dronecontrolapp.ui.theme.AerospaceBlue
import com.example.dronecontrolapp.ui.theme.ElectricCyan
import com.example.dronecontrolapp.ui.theme.OffWhite
import android.app.Application
import com.example.dronecontrolapp.MqttHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : ComponentActivity() {
    private val droneViewModel: DroneViewModel by viewModels { DroneViewModel.Factory(application) }
    private var useMqtt by mutableStateOf(true) // Define useMqtt as a mutable state at activity level

    // Add permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, we can show notifications
            AppLogger.info("Notification permission granted")
        } else {
            // Permission denied, inform the user about the implications
            AppLogger.warn("Notification permission denied - user will not receive important alerts")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission if needed (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
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
                        // Pass useMqtt state and its updater to MainApp
                        MainApp(
                            application = application,
                            useMqtt = useMqtt,
                            onUseMqttChange = { newValue -> useMqtt = newValue }
                        )
                    }
                }
            }
        }
    }

    private fun sendCommandToDrone(command: String) {
        if (useMqtt) {
            droneViewModel.sendCommand(command)
        } else {
            val phoneNumber = "+1234567890" // Replace with the drone's phone number
            SmsSender.sendCommand(phoneNumber, command)
        }
    }
}

@Composable
fun MainApp(
    application: Application,
    useMqtt: Boolean,
    onUseMqttChange: (Boolean) -> Unit
) {
    val droneViewModel: DroneViewModel = viewModel(
        factory = DroneViewModel.Factory(application)
    )

    var showSettings by remember { mutableStateOf(false) }

    // Ensure the connection settings are being used
    LaunchedEffect(Unit) {
        droneViewModel.connect()  // This will use the credentials set in DroneViewModel
    }

    // Observe connection status and update useMqtt
    LaunchedEffect(droneViewModel.connectionStatus.value) {
        if (droneViewModel.connectionStatus.value != "Connected") {
            onUseMqttChange(false)
            AppLogger.info("Switching to SMS communication")
        } else {
            onUseMqttChange(true)
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
                onBack = { showSettings = false },
                droneViewModel = droneViewModel
            )
        } else {
            DroneControlScreen(
                droneViewModel = droneViewModel,
                onNavigateToSettings = { showSettings = true },
                useMqtt = useMqtt
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneControlScreen(
    droneViewModel: DroneViewModel,
    onNavigateToSettings: () -> Unit,
    useMqtt: Boolean
) {
    var showLogViewer by remember { mutableStateOf(false) }
    var showDebugConsole by remember { mutableStateOf(false) }
    val isConnecting by droneViewModel.isConnecting
    val errorMessage by droneViewModel.errorMessage
    val dronePosition by droneViewModel.dronePosition

    // State for dropdown menu
    var expanded by remember { mutableStateOf(false) }

    // Full screen map with overlaid components
    Box(modifier = Modifier.fillMaxSize()) {
        // Map as the background layer
        EnhancedMapView(
            dronePosition = dronePosition,
            modifier = Modifier.fillMaxSize(),
            keepCentered = true  // Ensure this is always true for continuous tracking
        )

        // Additional telemetry data (detailed view)
        EnhancedTelemetrySection(
            battery = droneViewModel.battery.intValue,
            altitude = droneViewModel.altitude.doubleValue,
            speed = droneViewModel.speed.doubleValue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        // Bottom controls area
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            EnhancedControlsSection(
                viewModel = droneViewModel,
                useMqtt = useMqtt,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Accessibility icon for logs and settings at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = ElectricCyan // Change the color to Electric Cyan
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
                DropdownMenuItem(
                    onClick = {
                        showDebugConsole = true
                        expanded = false
                    },
                    text = { Text("MQTT Debug Console") }
                )
            }
        }

        // Display error message if present
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

        // Notifications overlay
        NotificationOverlay(
            logs = LogManager.activeNotifications,
            onDismiss = { LogManager.dismissNotification(it) },
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Log viewer dialog
        if (showLogViewer) {
            LogViewerDialog(
                onDismiss = { showLogViewer = false }
            )
        }

        // Add connection status indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    color = when {
                        isConnecting -> Color.Yellow.copy(alpha = 0.7f)
                        droneViewModel.connectionStatus.value == "Connected" -> 
                            Color.Green.copy(alpha = 0.7f)
                        else -> Color.Red.copy(alpha = 0.7f)
                    },
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isConnecting) "Connecting..." 
                      else droneViewModel.connectionStatus.value,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Add this after your other dialogs
        if (showDebugConsole) {
            MqttDebugConsole(
                droneViewModel = droneViewModel,
                onDismiss = { showDebugConsole = false }
            )
        }
    }
}

@Composable
fun ConnectionStatusBar(status: String, modifier: Modifier = Modifier) {
    val statusColor = when {
        status.startsWith("Connected") -> Color.Green // Green for connected
        status.startsWith("Connection failed") || status.startsWith("Exception") ->
            MaterialTheme.colorScheme.error
        else -> Color(0xFFD81B60) // Darker pink for other states
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = statusColor.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    status.startsWith("Connected") -> Icons.Default.CheckCircle
                    status.startsWith("Connection failed") || status.startsWith("Exception") ->
                        Icons.Default.Error
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = statusColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = status,
                color = statusColor,
                style = MaterialTheme.typography.bodySmall
            )
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
    onBack: () -> Unit,
    droneViewModel: DroneViewModel
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
                onClick = {
                    AppLogger.info("Settings saved - Broker URL: $broker")
                    onSave(broker, user, pass)
                },
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
fun EnhancedMapView(
    dronePosition: GeoPoint,
    modifier: Modifier = Modifier,
    keepCentered: Boolean = true
) {
    val context = LocalContext.current
    var mapController by remember { mutableStateOf<org.osmdroid.views.MapController?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Enhanced position tracking with center-following behavior
    LaunchedEffect(dronePosition) {
        AppLogger.debug("Drone position update: ${dronePosition.latitude}, ${dronePosition.longitude}")

        // Update marker position
        marker?.apply {
            position = dronePosition

            // Always center the map on the drone position
            if (keepCentered) {
                // Use animateTo for smooth transitions
                mapController?.animateTo(dronePosition)
            }

            // Force map redraw
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
                    // Increased size for better visibility
                    val width = 50  // Make it larger for better visibility
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
        update = { view ->
            // This ensures any updates to the map properly refresh
            view.invalidate()
        },
        modifier = modifier
    )

    // Only show recenter button if auto-centering is disabled
    if (!keepCentered) {
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = { mapController?.setCenter(dronePosition) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp),
                containerColor = Color(0xFFEC407A)
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Recenter",
                    tint = Color.White
                )
            }
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DroneControlAppTheme {
        val context = LocalContext.current.applicationContext as Application
        val droneViewModel = remember { DroneViewModel(context) }

        DroneControlScreen(
            droneViewModel = droneViewModel,
            onNavigateToSettings = {},
            useMqtt = true
        )
    }
}

@Composable
fun DroneControlScreenWithViewModel(onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current.applicationContext as Application
    val droneViewModel: DroneViewModel = viewModel(factory = DroneViewModel.Factory(context))

    DroneControlScreen(
        droneViewModel = droneViewModel,
        onNavigateToSettings = onNavigateToSettings,
        useMqtt = true
    )
}

@Composable
fun MqttDebugConsole(
    droneViewModel: DroneViewModel,
    onDismiss: () -> Unit
) {
    // Store a list of log messages
    val logs = remember { mutableStateListOf<String>() }
    
    // Set up a subscription for diagnostic logs
    LaunchedEffect(Unit) {
        // Add initial diagnostic info
        logs.add("🔎 MQTT Debug Console")
        logs.add("Broker: ${droneViewModel.brokerUrl.value}")
        logs.add("Connected: ${droneViewModel.isMqttConnected()}")
        logs.add("Status: ${droneViewModel.connectionStatus.value}")
        logs.add("Connection info: ${droneViewModel.getMqttConnectionInfo()}")
        
        // Test publishing/subscribing to verify broker functionality
        logs.add("Testing MQTT communication...")
        droneViewModel.testSubscriptions { message ->
            logs.add(message)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MQTT Debug Console") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                logs.forEach { log ->
                    Text(
                        log, 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Divider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    logs.add("Manual test initiated...")
                    droneViewModel.testSubscriptions { message ->
                        logs.add(message)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Test Subscriptions")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Close")
            }
        }
    )
}
