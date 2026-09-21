package com.example.floodgate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FloodGateColorScheme = lightColorScheme(
    primary = FloodGateAction,
    secondary = FloodGateAccent,
    onSecondary = FloodGateSurfacePrimary,
    surfaceVariant = FloodGateNeutral100,
    onSurfaceVariant = FloodGateNeutralDefault,
    surfaceTint = FloodGateAction,
    onPrimary = FloodGateTextPrimary,
    background = FloodGateSurfacePrimary,
    onBackground = FloodGateHeading,
    surface = FloodGateSurfacePrimary,
    onSurface = FloodGateHeading,
    outline = FloodGateBorderAction
)

private val FloodGateShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun FloodGateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FloodGateColorScheme,
        typography = Typography,
        shapes = FloodGateShapes,
        content = content
    )
}
