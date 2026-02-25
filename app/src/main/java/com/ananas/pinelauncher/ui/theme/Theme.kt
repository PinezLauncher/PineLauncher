package com.ananas.pinelauncher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    background = Color.Black,
    surface = Color.Black
)
private val LightColorScheme = darkColorScheme(
    background = Color.Black,
    surface = Color.Black
)
@Composable
fun PineLauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
@Composable
fun PineLauncherTheme(
    darkTheme: Unit,
    content: @Composable () -> Unit
) {


    MaterialTheme(
        typography = AppTypography,
        content = content
    )
}
