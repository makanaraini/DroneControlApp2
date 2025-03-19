package com.example.dronecontrolapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.dronecontrolapp.R.drawable.drone_icon
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import androidx.compose.ui.graphics.Color
import com.example.dronecontrolapp.AppLogger
import com.example.dronecontrolapp.R
import com.example.dronecontrolapp.ui.theme.AerospaceBlue
import com.example.dronecontrolapp.ui.theme.OffWhite

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
    
    // Enhanced position tracking with immediate center-following behavior
    LaunchedEffect(dronePosition) {
        AppLogger.debug("Drone position update: ${dronePosition.latitude}, ${dronePosition.longitude}")
        
        // Update marker position
        marker?.apply {
            position = dronePosition
            
            // Always center the map on the drone position with improved responsiveness
            if (keepCentered) {
                // Use setCenter for immediate positioning instead of animateTo to reduce lag
                mapController?.setCenter(dronePosition)
            }
            
            // Force immediate map redraw
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
                // Disable multi-touch controls when in tracking mode to prevent panning
                setMultiTouchControls(!keepCentered)
                mapView = this

                mapController = this.controller as org.osmdroid.views.MapController
                mapController?.setZoom(15.0)
                mapController?.setCenter(dronePosition)

                // Add a map motion listener to detect when user interacts with the map
                this.addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent): Boolean {
                        if (keepCentered) {
                            // Force recentering after user scroll interaction
                            mapController?.setCenter(dronePosition)
                        }
                        return true
                    }

                    override fun onZoom(event: ZoomEvent): Boolean {
                        if (keepCentered) {
                            // Force recentering after zoom changes
                            mapController?.setCenter(dronePosition)
                        }
                        return true
                    }
                })

                val originalIcon = ContextCompat.getDrawable(ctx, drone_icon)

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

    // Center crosshair indicator to show the exact center point
    if (keepCentered) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Center indicator that shows where the map is centered
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.Center)
                    .background(Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = AerospaceBlue.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            )
        }
    } else {
        // Only show recenter button if auto-centering is disabled
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = { mapController?.setCenter(dronePosition) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp),
                containerColor = AerospaceBlue
            ) {
                Icon(
                    Icons.Default.MyLocation, 
                    contentDescription = "Recenter",
                    tint = OffWhite
                )
            }
        }
    }
}

// Utility function for resizing bitmap
private fun getResizedBitmap(context: Context, drawableId: Int, width: Int, height: Int): BitmapDrawable {
    val original = ContextCompat.getDrawable(context, drawableId) as BitmapDrawable
    val resized = Bitmap.createScaledBitmap(original.bitmap, width, height, false)
    return BitmapDrawable(context.resources, resized)
}
