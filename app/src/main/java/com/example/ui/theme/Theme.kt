package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkTextPrimary,
    onSecondary = DarkTextPrimary,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightAccent,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val targetColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val primary = animateColorAsState(targetValue = targetColorScheme.primary, animationSpec = tween(durationMillis = 650), label = "primary")
    val secondary = animateColorAsState(targetValue = targetColorScheme.secondary, animationSpec = tween(durationMillis = 650), label = "secondary")
    val tertiary = animateColorAsState(targetValue = targetColorScheme.tertiary, animationSpec = tween(durationMillis = 650), label = "tertiary")
    val background = animateColorAsState(targetValue = targetColorScheme.background, animationSpec = tween(durationMillis = 650), label = "background")
    val surface = animateColorAsState(targetValue = targetColorScheme.surface, animationSpec = tween(durationMillis = 650), label = "surface")
    val onPrimary = animateColorAsState(targetValue = targetColorScheme.onPrimary, animationSpec = tween(durationMillis = 650), label = "onPrimary")
    val onSecondary = animateColorAsState(targetValue = targetColorScheme.onSecondary, animationSpec = tween(durationMillis = 650), label = "onSecondary")
    val onBackground = animateColorAsState(targetValue = targetColorScheme.onBackground, animationSpec = tween(durationMillis = 650), label = "onBackground")
    val onSurface = animateColorAsState(targetValue = targetColorScheme.onSurface, animationSpec = tween(durationMillis = 650), label = "onSurface")
    val surfaceVariant = animateColorAsState(targetValue = targetColorScheme.surfaceVariant, animationSpec = tween(durationMillis = 650), label = "surfaceVariant")
    val onSurfaceVariant = animateColorAsState(targetValue = targetColorScheme.onSurfaceVariant, animationSpec = tween(durationMillis = 650), label = "onSurfaceVariant")

    val animatedColorScheme = targetColorScheme.copy(
        primary = primary.value,
        secondary = secondary.value,
        tertiary = tertiary.value,
        background = background.value,
        surface = surface.value,
        onPrimary = onPrimary.value,
        onSecondary = onSecondary.value,
        onBackground = onBackground.value,
        onSurface = onSurface.value,
        surfaceVariant = surfaceVariant.value,
        onSurfaceVariant = onSurfaceVariant.value
    )

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography,
        content = content
    )
}
