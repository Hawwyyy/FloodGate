package com.example.floodgate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FloodGateColorScheme = lightColorScheme(
    primary = FloodGateAction,
    onPrimary = FloodGateTextPrimary,
    background = FloodGateSurfacePrimary,
    onBackground = FloodGateHeading,
    surface = FloodGateSurfacePrimary,
    onSurface = FloodGateHeading,
    outline = FloodGateBorderAction
)

private val FloodGateShapes = Shapes(
    medium = RoundedCornerShape(12.dp),
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
