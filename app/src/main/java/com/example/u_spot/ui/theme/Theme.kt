package com.example.u_spot.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = USpotPrimaryDark,
    secondary = USpotSecondaryDark,
    tertiary = USpotTertiaryDark,
    background = USpotBackgroundDark,
    surface = USpotSurfaceDark,
    onPrimary = USpotOnPrimaryDark,
    onBackground = USpotOnBackgroundDark,
    onSurface = USpotOnSurfaceDark,
    error = USpotErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary = USpotPrimaryLight,
    secondary = USpotSecondaryLight,
    tertiary = USpotTertiaryLight,
    background = USpotBackgroundLight,
    surface = USpotSurfaceLight,
    onPrimary = USpotOnPrimaryLight,
    onBackground = USpotOnBackgroundLight,
    onSurface = USpotOnSurfaceLight,
    error = USpotErrorLight,
)

@Composable
fun USpotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
