package com.example.floodgate.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.floodgate.R
import com.example.floodgate.ui.dashboard.BarrierCard
import com.example.floodgate.ui.dashboard.SystemStatusCard
import com.example.floodgate.ui.dashboard.WaterLevelCard
import com.example.floodgate.ui.theme.FloodGateTheme

@Composable
fun OnboardingMonitoringScreen(
    onNextClick: () -> Unit,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingFrame(modifier) { scale ->
        OnboardingHeading(
            title = stringResource(R.string.onboarding_real_time),
            accent = stringResource(R.string.onboarding_monitoring),
            description = stringResource(R.string.onboarding_monitoring_description),
            scale = scale,
            modifier = Modifier.align(Alignment.TopCenter)
                .offset(y = (OnboardingDesign.HeaderTop * scale).dp)
        )
        MonitoringStatusPreview(
            scale = scale,
            modifier = Modifier.align(Alignment.TopStart)
                .offset(
                    x = (MonitoringPreviewDesign.Left * scale).dp,
                    y = (MonitoringPreviewDesign.Top * scale).dp
                )
        )
        OnboardingProgress(1, onPageSelected, scale,
            Modifier.align(Alignment.TopCenter).offset(y = (OnboardingDesign.ProgressTouchTop * scale).dp))
        OnboardingPrimaryButton(stringResource(R.string.next), onNextClick, scale,
            Modifier.align(Alignment.TopCenter).offset(y = (OnboardingDesign.ButtonTop * scale).dp))
    }
}

/**
 * Static onboarding illustration from Figma. It intentionally reuses the dashboard's
 * status cards and exact exported assets, while the outer layer scales with the 412×917 frame.
 */
@Composable
private fun MonitoringStatusPreview(scale: Float, modifier: Modifier = Modifier) {
    val typography = MaterialTheme.typography
    val noFontPadding = PlatformTextStyle(includeFontPadding = false)
    MaterialTheme(
        typography = typography.copy(
            bodyLarge = typography.bodyLarge.copy(platformStyle = noFontPadding),
            bodyMedium = typography.bodyMedium.copy(platformStyle = noFontPadding),
            bodySmall = typography.bodySmall.copy(platformStyle = noFontPadding)
        )
    ) {
        Layout(
            modifier = modifier.requiredSize(
                (MonitoringPreviewDesign.Width * scale).dp,
                (MonitoringPreviewDesign.Height * scale).dp
            ),
            content = {
                Column(
                    Modifier.requiredSize(
                        MonitoringPreviewDesign.Width.dp,
                        MonitoringPreviewDesign.Height.dp
                    ).graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                ) {
                    BarrierCard(
                        Modifier.shadow(
                            elevation = MonitoringPreviewDesign.ShadowElevation.dp,
                            shape = RoundedCornerShape(MonitoringPreviewDesign.CardRadius.dp),
                            clip = false
                        )
                    )
                    Spacer(Modifier.height(MonitoringPreviewDesign.CardGap.dp))
                    Row(Modifier.requiredWidth(MonitoringPreviewDesign.Width.dp)) {
                        WaterLevelCard(Modifier.width(MonitoringPreviewDesign.WaterCardWidth.dp))
                        Spacer(Modifier.width(MonitoringPreviewDesign.MetricGap.dp))
                        SystemStatusCard(Modifier.width(MonitoringPreviewDesign.SystemCardWidth.dp))
                    }
                }
            }
        ) { measurables, constraints ->
            // Measure the Figma-sized content independently from the narrow parent. Placing it
            // explicitly at (0, 0) avoids Compose's default centering of an oversized child.
            val preview = measurables.single().measure(
                Constraints.fixed(
                    MonitoringPreviewDesign.Width.dp.roundToPx(),
                    MonitoringPreviewDesign.Height.dp.roundToPx()
                )
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                preview.placeRelative(0, 0)
            }
        }
    }
}

private object MonitoringPreviewDesign {
    const val Left = 16f
    const val Top = 294f
    const val Width = 380f
    const val Height = 440f
    const val CardGap = 16f
    const val WaterCardWidth = 190f
    const val MetricGap = 17f
    const val SystemCardWidth = 173f
    const val CardRadius = 8f
    const val ShadowElevation = 4f
}

@Preview(name = "Regular phone", widthDp = 412, heightDp = 917, showBackground = true)
@Preview(name = "Small phone", widthDp = 320, heightDp = 640, showBackground = true)
@Composable private fun OnboardingMonitoringPreview() {
    FloodGateTheme { OnboardingMonitoringScreen({}, {}) }
}
