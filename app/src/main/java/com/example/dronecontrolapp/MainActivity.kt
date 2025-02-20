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
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DroneControlAppUI()
                }
            }
        }
    }
}

@Composable
fun DroneControlAppUI() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Map Section
        Box(modifier = Modifier.weight(0.85f)) {
            OSMMapView(modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Controls Section
        ControlsSection(modifier = Modifier.weight(0.07f))

        Spacer(modifier = Modifier.height(4.dp))

        // Telemetry Section
        TelemetrySection(modifier = Modifier.weight(0.08f))
    }
}

@Composable
fun OSMMapView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var dronePosition by remember { mutableStateOf(GeoPoint(-1.286389, 36.817223)) }
    var mapController by remember { mutableStateOf<org.osmdroid.views.MapController?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }

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
                    mapController?.setCenter(dronePosition)

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
                        position = dronePosition
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
            onClick = { mapController?.setCenter(dronePosition) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(40.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Recenter", modifier = Modifier.size(20.dp))
        }

        // Comment out this section to stop automatic movement
        /*
        LaunchedEffect(Unit) {
            while (true) {
                delay(2000)
                dronePosition = GeoPoint(
                    dronePosition.latitude + 0.001,
                    dronePosition.longitude + 0.001
                )
                marker?.position = dronePosition
                mapController?.setCenter(dronePosition)
            }
        }
        */
    }
}
@Composable
fun ControlsSection(modifier: Modifier = Modifier) {
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
        // Takeoff Button with Animation
        Button(
            onClick = { 
                coroutineScope.launch {
                    takeoffScale.animateTo(0.9f, animationSpec = spring())
                    takeoffScale.animateTo(1f, animationSpec = spring())
                }
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

        // Land Button with Animation
        Button(
            onClick = {
                coroutineScope.launch {
                    landScale.animateTo(0.9f, animationSpec = spring())
                    landScale.animateTo(1f, animationSpec = spring())
                }
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
fun TelemetrySection(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.medium)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Altitude
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.Height, contentDescription = "Altitude", modifier = Modifier.size(16.dp))
            Text("Altitude", style = MaterialTheme.typography.bodySmall)
            Text("0m", style = MaterialTheme.typography.bodySmall)
        }

        // Speed
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.Speed, contentDescription = "Speed", modifier = Modifier.size(16.dp))
            Text("Speed", style = MaterialTheme.typography.bodySmall)
            Text("0m/s", style = MaterialTheme.typography.bodySmall)
        }

        // Battery
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.BatteryFull, contentDescription = "Battery", modifier = Modifier.size(16.dp))
            Text("Battery", style = MaterialTheme.typography.bodySmall)
            Text("100%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DroneControlAppTheme {
        DroneControlAppUI()
    }
}