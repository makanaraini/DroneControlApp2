package com.example.dronecontrolapp

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .padding(16.dp)
    ) {
        // Map Section
        OSMMapView(modifier = Modifier.weight(0.75f))

        Spacer(modifier = Modifier.height(16.dp))

        // Controls Section
        ControlsSection(modifier = Modifier.weight(0.11f))

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry Section
        TelemetrySection(modifier = Modifier.weight(0.14f))
    }
}

@Composable
fun OSMMapView(modifier: Modifier = Modifier) {
    val context = LocalContext.current

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

                val mapController = this.controller
                mapController.setZoom(15.0)
                val startPoint = GeoPoint(-1.286389, 36.817223)
                mapController.setCenter(startPoint)

                val marker = Marker(this)
                marker.position = startPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Nairobi"
                this.overlays.add(marker)

                this.invalidate()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .shadow(4.dp, shape = MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
    )
}

@Composable
fun ControlsSection(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Takeoff Button with Icon
        Button(
            onClick = { /* Handle takeoff */ },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.FlightTakeoff, contentDescription = "Takeoff")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Takeoff")
        }

        // Land Button with Icon
        Button(
            onClick = { /* Handle land */ },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.FlightLand, contentDescription = "Land")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Land")
        }
    }
}

@Composable
fun TelemetrySection(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Altitude
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Height, contentDescription = "Altitude")
            Text("Altitude", style = MaterialTheme.typography.labelSmall)
            Text("0m", style = MaterialTheme.typography.titleMedium)
        }

        // Speed
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Speed, contentDescription = "Speed")
            Text("Speed", style = MaterialTheme.typography.labelSmall)
            Text("0m/s", style = MaterialTheme.typography.titleMedium)
        }

        // Battery
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.BatteryFull, contentDescription = "Battery")
            Text("Battery", style = MaterialTheme.typography.labelSmall)
            Text("100%", style = MaterialTheme.typography.titleMedium)
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