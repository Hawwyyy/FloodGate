package com.example.floodgate.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.floodgate.ui.theme.FloodGateBrandFloodInitial
import com.example.floodgate.ui.theme.FloodGateBrandFloodRemainder
import com.example.floodgate.ui.theme.FloodGateBrandGateInitial
import com.example.floodgate.ui.theme.FloodGateBrandGateRemainder
import com.example.floodgate.ui.theme.FloodGateBrandLine
import com.example.floodgate.ui.theme.FloodGateHeading

@Composable
fun FloodGateBrandName(
    modifier: Modifier = Modifier,
    color: Color = FloodGateHeading
) {
    val brandName = buildAnnotatedString {
        withStyle(FloodGateBrandFloodInitial) { append("F") }
        withStyle(FloodGateBrandFloodRemainder) { append("lood") }
        withStyle(FloodGateBrandGateInitial) { append("G") }
        withStyle(FloodGateBrandGateRemainder) { append("ate") }
    }

    Text(
        text = brandName,
        modifier = modifier,
        color = color,
        style = FloodGateBrandLine,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false
    )
}

object FloodGateBrandDimensions {
    val Width = 254.dp
    val Height = 88.dp
}
