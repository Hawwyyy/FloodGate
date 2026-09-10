package com.example.floodgate.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floodgate.R
import com.example.floodgate.ui.theme.*

@Composable
fun OnboardingAlertsScreen(
    onGetStartedClick: () -> Unit,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingFrame(modifier) { scale ->
        OnboardingHeading(
            title = stringResource(R.string.onboarding_instant_alerts),
            accent = stringResource(R.string.onboarding_peace_of_mind),
            description = stringResource(R.string.onboarding_alerts_description),
            scale = scale,
            width = 380f,
            modifier = Modifier.align(Alignment.TopCenter)
                .offset(y = (OnboardingDesign.HeaderTop * scale).dp)
        )
        AlertPreviewCard(R.drawable.onboarding_alert_warning, R.string.onboarding_flood_alert,
            R.string.onboarding_flood_alert_description, scale,
            Modifier.offset(x = (16f * scale).dp, y = (322f * scale).dp))
        AlertPreviewCard(R.drawable.onboarding_alert_shield, R.string.onboarding_barrier_deploying,
            R.string.onboarding_barrier_deploying_description, scale,
            Modifier.offset(x = (115f * scale).dp, y = (420f * scale).dp))
        AlertPreviewCard(R.drawable.onboarding_alert_check, R.string.onboarding_barrier_secured,
            R.string.onboarding_barrier_secured_description, scale,
            Modifier.offset(x = (16f * scale).dp, y = (518f * scale).dp))
        OnboardingProgress(2, onPageSelected, scale,
            Modifier.align(Alignment.TopCenter).offset(y = (OnboardingDesign.ProgressTouchTop * scale).dp))
        OnboardingPrimaryButton(stringResource(R.string.get_started), onGetStartedClick, scale,
            Modifier.align(Alignment.TopCenter).offset(y = (OnboardingDesign.ButtonTop * scale).dp))
    }
}

@Composable
private fun AlertPreviewCard(
    @DrawableRes icon: Int,
    title: Int,
    description: Int,
    scale: Float,
    modifier: Modifier
) {
    Row(modifier.width((281f * scale).dp).shadow((4f * scale).dp,
        RoundedCornerShape((8f * scale).dp)).clip(RoundedCornerShape((8f * scale).dp))
        .background(FloodGateNeutral100).padding((8f * scale).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((16f * scale).dp)) {
        Box(Modifier.width((40f * scale).dp).height((34f * scale).dp)
            .clip(RoundedCornerShape((4f * scale).dp)).background(OnboardingLightBlue)
            .padding((2f * scale).dp).clip(RoundedCornerShape((2f * scale).dp))) {
            Image(painterResource(R.drawable.onboarding_alert_logo), null,
                Modifier.fillMaxSize().offset(x = (-6.46f * scale).dp, y = (-1.94f * scale).dp)
                    .requiredSize((48f * scale).dp, (33.87f * scale).dp),
                contentScale = ContentScale.FillBounds)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy((16f * scale).dp),
            verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((10f * scale).dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((8f * scale).dp)) {
                    Image(painterResource(icon), null, Modifier.size((20f * scale).dp))
                    Text(stringResource(title), color = FloodGateHeading,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (16f * scale).sp, lineHeight = (20f * scale).sp),
                        fontWeight = FontWeight.Medium)
                }
                Text(stringResource(description), color = OnboardingBody,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = (12f * scale).sp, lineHeight = (14f * scale).sp))
            }
            Text(stringResource(R.string.onboarding_now), color = FloodGateNeutral400,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = (12f * scale).sp, lineHeight = (14f * scale).sp))
        }
    }
}

private val OnboardingLightBlue = Color(0xFF90D5FF)
private val OnboardingBody = Color(0xFF111215)

@Preview(name = "Regular phone", widthDp = 412, heightDp = 917, showBackground = true)
@Preview(name = "Small phone", widthDp = 320, heightDp = 640, showBackground = true)
@Composable private fun OnboardingAlertsPreview() {
    FloodGateTheme { OnboardingAlertsScreen({}, {}) }
}
