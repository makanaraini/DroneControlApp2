package com.example.dronecontrolapp

import android.os.Bundle
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.Button
import android.widget.TextView
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val serverUrl = "http://your-server-ip:3000" // Change this to your server IP
    private val scope = CoroutineScope(Dispatchers.IO)
    
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
<<<<<<< HEAD
                    var showSplash by remember { mutableStateOf(true) }
                    
                    if (showSplash) {
                        SplashScreen(onLoadingComplete = { showSplash = false })
                    } else {
                        MainApp()
=======
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
>>>>>>> 65bd6f75ee10f0bbd19d76a9c29da061912f5d0f
                    }
                }
            }
        }
        
        // Set up control buttons
        findViewById<Button>(R.id.btnTakeoff).setOnClickListener {
            sendControlCommand("TAKEOFF")
            updateLogs("Sent TAKEOFF command to drone")
        }
        
        findViewById<Button>(R.id.btnLand).setOnClickListener {
            sendControlCommand("LAND")
            updateLogs("Sent LAND command to drone")
        }
        
        // Start periodic telemetry updates
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
                            updateLogs("Failed to send command: ${response.code}")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateLogs("Error sending command: ${e.message}")
                }
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
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            updateLogs("Failed to send telemetry: ${response.code}")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateLogs("Error sending telemetry: ${e.message}")
                }
            }
        }
    }
    
    private fun startTelemetryUpdates() {
        scope.launch {
            while (true) {
                // Fetch telemetry data from your drone
                // This is placeholder - replace with your actual drone telemetry code
                val telemetry = JSONObject().apply {
                    put("speed", getDroneSpeed())
                    put("battery", getDroneBattery())
                    put("altitude", getDroneAltitude())
                    put("latitude", getDroneLatitude())
                    put("longitude", getDroneLongitude())
                }
                
                // Send telemetry to server
                sendTelemetryData(telemetry)
                
                // Wait before next update
                delay(1000) // Update every second
            }
        }
    }
    
    // Placeholder functions - replace with actual drone data retrieval methods
    private fun getDroneSpeed(): Float = 5.0f
    private fun getDroneBattery(): Int = 80
    private fun getDroneAltitude(): Float = 10.0f
    private fun getDroneLatitude(): Double = 37.7749
    private fun getDroneLongitude(): Double = -122.4194
    
    private fun updateLogs(message: String) {
        // Update your UI logs here
        // For example:
        val logsTextView = findViewById<TextView>(R.id.tvLogs)
        logsTextView.append("$message\n")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel() // Cancel all coroutines when the activity is destroyed
    }
}

@Composable
<<<<<<< HEAD
fun MainApp() {
    val droneViewModel: DroneViewModel = viewModel(
        factory = DroneViewModel.Factory(LocalContext.current)
    )
    
    var showSettings by remember { mutableStateOf(false) }
    
    // Initialize connection on first launch if not already connected
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
=======
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
>>>>>>> 65bd6f75ee10f0bbd19d76a9c29da061912f5d0f
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

<<<<<<< HEAD
        // Log viewer dialog
        if (showLogViewer) {
            LogViewerDialog(
                onDismiss = { showLogViewer = false }
            )
=======
            // Controls Section
            ControlsSection(mqttHandler, modifier = Modifier.weight(0.07f))

            // Telemetry Section
            TelemetrySection(battery, altitude, speed, modifier = Modifier.weight(0.08f))
>>>>>>> 65bd6f75ee10f0bbd19d76a9c29da061912f5d0f
        }
    }
}

@Composable
<<<<<<< HEAD
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
=======
fun OSMMapView(
    dronePosition: GeoPoint,
    modifier: Modifier = Modifier
>>>>>>> 65bd6f75ee10f0bbd19d76a9c29da061912f5d0f
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

    // Load OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

<<<<<<< HEAD
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
=======
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
>>>>>>> 65bd6f75ee10f0bbd19d76a9c29da061912f5d0f
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
<<<<<<< HEAD
fun EnhancedControlsSection(viewModel: DroneViewModel, modifier: Modifier = Modifier) {
    val takeoffScale = remember { Animatable(1f) }
    val landScale = remember { Animatable(1f) }
=======
fun ControlsSection(mqttHandler: MqttHandler, modifier: Modifier = Modifier) {
>>>>>>> 65bd6f75ee10f0bbd19d76a9c29da061912f5d0f
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
<<<<<<< HEAD
        // Takeoff Button
        Button(
            onClick = { 
                coroutineScope.launch {
                    takeoffScale.animateTo(0.9f, animationSpec = spring(stiffness = 300f))
                    takeoffScale.animateTo(1f, animationSpec = spring(stiffness = 300f))
                }
                AppLogger.info("User command: Takeoff initiated")
                viewModel.sendCommand("takeoff")
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .scale(takeoffScale.value)
                .height(32.dp),
=======
        // Reusable composable function for animated buttons
        fun AnimatedButton(
            label: String,
            icon: ImageVector,
            contentDescription: String,
            command: String,
            buttonColor: Color,
            scale: Animatable
>>>>>>> 65bd6f75ee10f0bbd19d76a9c29da061912f5d0f
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
<<<<<<< HEAD
        Button(
            onClick = {
                coroutineScope.launch {
                    landScale.animateTo(0.9f, animationSpec = spring(stiffness = 300f))
                    landScale.animateTo(1f, animationSpec = spring(stiffness = 300f))
                }
                AppLogger.info("User command: Land initiated")
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
=======
        val landScale = remember { Animatable(1f) }
        AnimatedButton(
            label = "Land",
            icon = Icons.Default.FlightLand,
            contentDescription = "Land",
            command = "land",
            buttonColor = MaterialTheme.colorScheme.error,
            scale = landScale
        )
>>>>>>> 65bd6f75ee10f0bbd19d76a9c29da061912f5d0f
    }
}

@Composable
fun EnhancedTelemetrySection(
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
<<<<<<< HEAD
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
=======
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
>>>>>>> 65bd6f75ee10f0bbd19d76a9c29da061912f5d0f
    }
}

@Composable
private fun TelemetryItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
<<<<<<< HEAD
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
=======
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
>>>>>>> 65bd6f75ee10f0bbd19d76a9c29da061912f5d0f
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DroneControlAppTheme {
        DroneControlScreen(
            droneViewModel = DroneViewModel(LocalContext.current),
            onNavigateToSettings = {}  // Add empty function for preview
        )
    }
}
