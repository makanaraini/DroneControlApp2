package com.example.dronecontrolapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Google Maps Light Mode Color Palette
private val md_theme_light_primary = Color(0xFF4285F4)        // Google Blue
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)      // White
private val md_theme_light_secondary = Color(0xFF34A853)      // Google Green
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)    // White
private val md_theme_light_error = Color(0xFFEA4335)          // Google Red
private val md_theme_light_background = Color(0xFFF8F9FA)     // Google Light Gray
private val md_theme_light_surface = Color(0xFFE8EAED)        // Google Slightly Darker Gray
private val md_theme_light_tertiaryContainer = Color(0xFFFBBC05) // Google Yellow

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primary,
    onPrimaryContainer = md_theme_light_onPrimary,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondary,
    onSecondaryContainer = md_theme_light_onPrimary,
    background = md_theme_light_background,
    surface = md_theme_light_surface,
    error = md_theme_light_error,
    onError = md_theme_light_onPrimary
)

// Dark Theme - Instagram Dark Mode
private val md_theme_dark_primary = Color(0xFF607D8B)       // Darker Instagram Purple
private val md_theme_dark_onPrimary = Color(0xFF833AB4)      // Instagram Purple
private val md_theme_dark_secondary = Color(0xFFE1306C)      // Instagram Pink
private val md_theme_dark_onSecondary = Color(0xFFFFFFFF)    // White
private val md_theme_dark_error = Color(0xFFFF5733)          // Warning Red-Orange
private val md_theme_dark_background = Color(0xFF262626)     // Dark Gray (Instagram Dark)
private val md_theme_dark_surface = Color(0xFF121212)        // Even Darker Gray
private val md_theme_dark_tertiaryContainer = Color(0xFFFCAF45) // Instagram Yellow
private val md_theme_dark_onTertiaryContainer = Color(0xFFE6E6E6) // Light Gray Text

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primary,
    onPrimaryContainer = md_theme_dark_onPrimary,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondary,
    onSecondaryContainer = md_theme_dark_onPrimary,
    background = md_theme_dark_background,
    surface = md_theme_dark_surface,
    error = md_theme_dark_error,
    onError = md_theme_dark_onPrimary
)

// Modern Typography
val ModernTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp
    )
)

@Composable
fun DroneControlAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ModernTypography,
        content = content
    )
}