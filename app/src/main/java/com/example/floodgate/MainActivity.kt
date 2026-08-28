package com.example.floodgate

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.floodgate.ui.theme.FloodGateAction
import com.example.floodgate.ui.components.FloodGateBrandDimensions
import com.example.floodgate.ui.components.FloodGateBrandName
import com.example.floodgate.ui.theme.FloodGateSpacing
import com.example.floodgate.ui.theme.FloodGateSplashOverlayBottom
import com.example.floodgate.ui.theme.FloodGateSplashOverlayTop
import com.example.floodgate.ui.theme.FloodGateTextPrimary
import com.example.floodgate.ui.theme.FloodGateTheme
import com.example.floodgate.ui.auth.FloodGateAuthApp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdgeWindow(useDarkSystemIcons = true)

        setContent {
            FloodGateTheme {
                FloodGateAuthApp(onExit = ::finish)
            }
        }
    }

    private fun configureEdgeToEdgeWindow(useDarkSystemIcons: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = useDarkSystemIcons
            isAppearanceLightNavigationBars = useDarkSystemIcons
        }
    }
}

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(FloodGateAction)
        ) {
            FigmaSplashBackground()

            FloodGateBrand(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun FigmaSplashBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Layout(
            content = {
                Image(
                    painter = painterResource(R.drawable.splash_screen_background),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { measurables, constraints ->
            val designScale = constraints.maxWidth / SplashDesign.FrameWidth
            val backgroundWidth =
                (SplashDesign.BackgroundWidth * designScale).roundToInt()
            val backgroundHeight =
                (SplashDesign.BackgroundHeight * designScale).roundToInt()
            val backgroundX = (SplashDesign.BackgroundX * designScale).roundToInt()
            val image = measurables.first().measure(
                Constraints.fixed(backgroundWidth, backgroundHeight)
            )

            layout(constraints.maxWidth, constraints.maxHeight) {
                image.placeRelative(backgroundX, 0)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            FloodGateSplashOverlayTop,
                            FloodGateSplashOverlayBottom
                        )
                    )
                )
        )
    }
}

@Composable
private fun FloodGateBrand(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(SplashDesign.BrandWidth.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FloodGateSpacing.Xxs)
    ) {
        FloodGateBrandName(
            modifier = Modifier.width(FloodGateBrandDimensions.Width),
            color = FloodGateTextPrimary
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FloodGateSpacing.Xxs)
        ) {
            Text(
                text = stringResource(R.string.smart_protection),
                color = FloodGateTextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.real_time_alerts),
                color = FloodGateTextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private object SplashDesign {
    const val FrameWidth = 412f
    const val BackgroundX = -783f
    const val BackgroundWidth = 1829f
    const val BackgroundHeight = 1228f
    const val BrandWidth = 254f
}

@Preview(
    name = "Splash Screen",
    widthDp = 412,
    heightDp = 917,
    showBackground = true
)
@Composable
private fun SplashScreenPreview() {
    FloodGateTheme {
        SplashScreen()
    }
}
