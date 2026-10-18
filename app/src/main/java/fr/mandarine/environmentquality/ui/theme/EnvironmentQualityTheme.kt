package fr.mandarine.environmentquality.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E8A),
    onPrimary = Color.White,
    secondary = Color(0xFF2E7D5B),
    onSecondary = Color.White,
    background = Color(0xFFF7FAF9),
    surface = Color(0xFFF7FAF9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD0EA),
    onPrimary = Color(0xFF00384A),
    secondary = Color(0xFF8BD6B0),
    onSecondary = Color(0xFF00391F),
)

@Composable
fun EnvironmentQualityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
