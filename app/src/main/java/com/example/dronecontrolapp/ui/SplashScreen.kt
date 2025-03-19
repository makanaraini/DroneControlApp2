package com.example.dronecontrolapp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin
import com.example.dronecontrolapp.R

@Composable
fun FuturisticSplashScreen(onLoadingComplete: () -> Unit) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    val rotationAngle = remember { Animatable(0f) }
    val orbitPosition by rememberInfiniteTransition(label = "orbit").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)),
        label = "orbit"
    )

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        alpha.animateTo(1f, animationSpec = tween(1000))
        rotationAngle.animateTo(360f, animationSpec = tween(1500))
        delay(1500)
        onLoadingComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        OrbitAnimation(orbitPosition)
        SplashContent(scale, alpha)
    }
}

@Composable
private fun OrbitAnimation(orbitPosition: Float) {
    val orbitRadius = 100f
    repeat(8) { index ->
        val angle = (orbitPosition + index * (360f / 8)) % 360f
        val x = orbitRadius * cos(angle.toRadians()).toFloat()
        val y = orbitRadius * sin(angle.toRadians()).toFloat()

        Box(
            modifier = Modifier
                .offset(x.dp, y.dp)
                .size(8.dp)
                .alpha(0.6f + (index % 3) * 0.2f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary)
        )
    }
}

@Composable
private fun SplashContent(scale: Animatable<Float, AnimationVector1D>, alpha: Animatable<Float, AnimationVector1D>) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.drone_icon),
            contentDescription = "Drone Logo",
            modifier = Modifier
                .size(80.dp)
                .scale(scale.value)
                .alpha(alpha.value)
                .graphicsLayer(rotationZ = scale.value * 360)
        )
    }
}

private fun Float.toRadians() = toRadians(this.toDouble())
