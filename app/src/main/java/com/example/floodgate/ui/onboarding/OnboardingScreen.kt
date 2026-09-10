package com.example.floodgate.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floodgate.R
import com.example.floodgate.ui.theme.*

@Composable
fun OnboardingScreen(
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPageSelected: (Int) -> Unit = {}
) {
    OnboardingFrame(modifier) { scale ->
        OnboardingHeading(
            title = stringResource(R.string.onboarding_smart_flood),
            accent = stringResource(R.string.onboarding_protection),
            description = stringResource(R.string.onboarding_protection_description),
            scale = scale,
            modifier = Modifier.align(Alignment.TopCenter)
                .offset(y = (OnboardingDesign.HeaderTop * scale).dp)
        )
        Image(
            painter = painterResource(R.drawable.onboarding_flood_barrier),
            contentDescription = stringResource(R.string.onboarding_flood_barrier_description),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.align(Alignment.TopCenter)
                .offset(y = (OnboardingDesign.IllustrationTop * scale).dp)
                .requiredWidth((OnboardingDesign.IllustrationWidth * scale).dp)
                .height((OnboardingDesign.IllustrationHeight * scale).dp)
        )
        OnboardingProgress(0, onPageSelected, scale,
            Modifier.align(Alignment.TopCenter).offset(y = (OnboardingDesign.ProgressTouchTop * scale).dp))
        OnboardingPrimaryButton(stringResource(R.string.next), onNextClick, scale,
            Modifier.align(Alignment.TopCenter).offset(y = (OnboardingDesign.ButtonTop * scale).dp))
    }
}

@Composable
internal fun OnboardingFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(scale: Float) -> Unit
) {
    Box(modifier.fillMaxSize().background(Color.Black)) {
        BoxWithConstraints(
            Modifier.matchParentSize().clip(MaterialTheme.shapes.extraLarge)
                .background(FloodGateSurfacePrimary)
        ) {
            val scale = minOf(maxWidth.value / OnboardingDesign.FrameWidth,
                maxHeight.value / OnboardingDesign.FrameHeight)
            Box(
                Modifier.requiredSize((OnboardingDesign.FrameWidth * scale).dp,
                    (OnboardingDesign.FrameHeight * scale).dp).align(Alignment.Center),
                content = { content(scale) }
            )
        }
    }
}

@Composable
internal fun OnboardingHeading(
    title: String,
    accent: String,
    description: String,
    scale: Float,
    modifier: Modifier = Modifier,
    width: Float = OnboardingDesign.HeaderWidth
) {
    Column(modifier.width((width * scale).dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((OnboardingDesign.HeaderGap * scale).dp)) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = FloodGateHeading)) { append(title); append('\n') }
                withStyle(SpanStyle(color = FloodGateAccent)) { append(accent) }
            },
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = (OnboardingDesign.HeadingFontSize * scale).sp,
                lineHeight = (OnboardingDesign.HeadingLineHeight * scale).sp),
            textAlign = TextAlign.Center
        )
        Text(description, color = FloodGateNeutral400,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = (OnboardingDesign.BodyFontSize * scale).sp,
                lineHeight = (OnboardingDesign.BodyLineHeight * scale).sp),
            textAlign = TextAlign.Center)
    }
}

@Composable
internal fun OnboardingProgress(
    activePage: Int,
    onPageSelected: (Int) -> Unit,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Row(modifier.width((OnboardingDesign.ProgressTouchWidth * scale).dp)
        .height((OnboardingDesign.ProgressTouchHeight * scale).dp).testTag("onboarding_progress"),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(OnboardingDesign.PageCount) { page ->
            val label = stringResource(R.string.onboarding_go_to_page, page + 1)
            Box(
                Modifier.width((OnboardingDesign.ProgressTouchCellWidth * scale).dp)
                    .fillMaxHeight().semantics {
                        contentDescription = label
                        selected = page == activePage
                    }.clickable(role = Role.Tab) { onPageSelected(page) },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size((OnboardingDesign.ProgressDotSize * scale).dp)
                    .background(if (page == activePage) FloodGateProgressActive else FloodGateProgressInactive,
                        CircleShape))
            }
        }
    }
}

@Composable
internal fun OnboardingPrimaryButton(
    label: String,
    onClick: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier.width((OnboardingDesign.ButtonWidth * scale).dp)
        .height((OnboardingDesign.ButtonHeight * scale).dp)
        .clip(RoundedCornerShape((OnboardingDesign.ButtonRadius * scale).dp))
        .background(FloodGateAction).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(label, color = FloodGateTextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily,
                fontSize = (OnboardingDesign.BodyFontSize * scale).sp,
                lineHeight = (OnboardingDesign.BodyLineHeight * scale).sp))
    }
}

internal object OnboardingDesign {
    const val FrameWidth = 412f
    const val FrameHeight = 917f
    const val HeaderTop = 118f
    const val HeaderWidth = 251f
    const val HeaderGap = 8f
    const val HeadingFontSize = 40f
    const val HeadingLineHeight = 48f
    const val BodyFontSize = 16f
    const val BodyLineHeight = 20f
    const val IllustrationTop = 318f
    const val IllustrationWidth = 676f
    const val IllustrationHeight = 451f
    const val ProgressTouchTop = 757.5f
    const val ProgressTouchWidth = 93f
    const val ProgressTouchHeight = 48f
    const val ProgressTouchCellWidth = 31f
    const val ProgressDotSize = 15f
    const val PageCount = 3
    const val ButtonTop = 805f
    const val ButtonWidth = 380f
    const val ButtonHeight = 52f
    const val ButtonRadius = 12f
}

@Preview(widthDp = 412, heightDp = 917, showBackground = true)
@Composable private fun OnboardingScreenPreview() {
    FloodGateTheme { OnboardingScreen({}) }
}
