package com.lasertrac.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Added global color definitions
val TopBarColor = Color(0xFF303030)
val TextColorLight = Color.White
val DashboardIconCircleBg = Color.Gray.copy(alpha = 0.6f) // Added this line

// Updated Light Color Scheme using our new colors
private val LightColorPalette = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentTeal,
    tertiary = Pink40, // Existing, can be changed if needed
    background = LightGrayBackground,
    surface = CardBackground,
    onPrimary = Color.White,
    onSecondary = Color.White, // Text on AccentTeal
    onTertiary = Color.White,
    onBackground = DarkGrayText,
    onSurface = DarkGrayText
)

// Updated Dark Color Scheme (adjust as needed for ideal dark theme contrast)
private val DarkColorPalette = darkColorScheme(
    primary = PrimaryBlue, // Or a lighter blue variant for dark theme like Purple80
    secondary = AccentTeal, // Or a lighter teal variant
    tertiary = Pink80, // Existing
    background = Color(0xFF121212), // Common dark theme background
    surface = Color(0xFF1E1E1E),    // Common dark theme surface (e.g., for cards)
    onPrimary = Color.White,
    onSecondary = Color.Black, // Text on AccentTeal (if AccentTeal is light enough)
    onTertiary = Color.Black,
    onBackground = Color(0xFFE0E0E0), // Light text for dark backgrounds
    onSurface = Color(0xFFE0E0E0)     // Light text for dark surfaces
)

@Composable
fun Lasertac2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic coloring is available on Android 12+
    dynamicColor: Boolean = false, // Set to true if you want to support Material You
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorPalette
        else -> LightColorPalette
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming Typography.kt is defined
        content = content
    )
}
