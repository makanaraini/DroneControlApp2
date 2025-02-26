package com.example.dronecontrolapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.dronecontrolapp.R
import com.example.dronecontrolapp.R.drawable.drone_icon
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun EnhancedMapView(dronePosition: GeoPoint, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mapController by remember { mutableStateOf<org.osmdroid.views.MapController?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var animatedPosition by remember { mutableStateOf(dronePosition) }
    
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(dronePosition) {
        // Animate the marker's position
        coroutineScope.launch {
            marker?.position = dronePosition
            mapView?.invalidate()
        }
        animatedPosition = dronePosition
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    Box(modifier = modifier) {
        // Map View
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    mapController = controller as MapController?
                    mapController?.setCenter(dronePosition)

                    // Add marker
                    marker = Marker(this).apply {
                        position = dronePosition
                        val originalIcon = ContextCompat.getDrawable(ctx, drone_icon) as BitmapDrawable
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            originalIcon.bitmap,
                            50, // specify the new width
                            50, // specify the new height
                            false
                        )
                        icon = BitmapDrawable(ctx.resources, scaledBitmap)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    overlays.add(marker)
                    mapView = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .shadow(4.dp, shape = MaterialTheme.shapes.medium)
        )

        // Recenter Button
        FloatingActionButton(
            onClick = { 
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