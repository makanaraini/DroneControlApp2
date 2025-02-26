package com.example.dronecontrolapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FuturisticSplashScreen(onLoadingComplete: () -> Unit) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    val rotationAngle = remember { Animatable(0f) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val orbitPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )
    
    val orbitRadius = 100f
    
    LaunchedEffect(Unit) {
        // Animate the logo
        scale.animateTo(1f, animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ))
        alpha.animateTo(1f, animationSpec = tween(1000))
        rotationAngle.animateTo(360f, animationSpec = tween(1500))

        delay(1500) // Additional delay after animation
        onLoadingComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.primary
                    ),
                    center = Offset(500f, 500f),
                    radius = 1000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Orbit dots
        repeat(8) { index ->
            val angle = (orbitPosition + index * (360f / 8)) % 360f
            val x = orbitRadius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = orbitRadius * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
            
            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(8.dp)
                    .alpha(0.6f + (index % 3) * 0.2f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary)
            )
        }
        
        // Center logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Flight,
                    contentDescription = "Drone Logo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale.value)
                        .alpha(alpha.value)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "DRONE CONTROL",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alpha.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "INITIALIZING SYSTEMS",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.alpha(alpha.value)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            LinearProgressIndicator(
                modifier = Modifier
                    .width(200.dp)
                    .alpha(alpha.value),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
            )
        }
    }
} 