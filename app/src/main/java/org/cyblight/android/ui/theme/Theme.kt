package org.cyblight.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = CybOrange,
    onPrimary = Color.White,
    secondary = CybGreen,
    onSecondary = Color.White,
    tertiary = CybCyan,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE8ECF4),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFF9CA3AF),
)

private val DarkColorSchemeWithOutline = darkColorScheme(
    primary = CybOrange,
    onPrimary = Color.White,
    secondary = CybGreen,
    onSecondary = Color.White,
    tertiary = CybCyan,
    background = CybDarkBackground,
    onBackground = Color.White,
    surface = CybDarkSurface,
    onSurface = Color.White,
    surfaceVariant = CybDarkSurfaceVariant,
    onSurfaceVariant = CybTextMuted,
    outline = CybTextMuted,
)

@Composable
fun CybLightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorSchemeWithOutline
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
