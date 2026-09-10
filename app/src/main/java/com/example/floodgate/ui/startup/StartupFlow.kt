package com.example.floodgate.ui.startup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.floodgate.SplashScreen
import com.example.floodgate.data.onboarding.OnboardingPreferences
import com.example.floodgate.ui.onboarding.OnboardingScreen
import com.example.floodgate.ui.onboarding.OnboardingAlertsScreen
import com.example.floodgate.ui.onboarding.OnboardingMonitoringScreen

private val AvailableOnboardingPages: List<@Composable (
    onNext: () -> Unit,
    onPageSelected: (Int) -> Unit
) -> Unit> = listOf(
    { onNext, onPageSelected ->
        OnboardingScreen(onNextClick = onNext, onPageSelected = onPageSelected)
    },
    { onNext, onPageSelected ->
        OnboardingMonitoringScreen(onNextClick = onNext, onPageSelected = onPageSelected)
    },
    { onNext, onPageSelected ->
        OnboardingAlertsScreen(onGetStartedClick = onNext, onPageSelected = onPageSelected)
    }
)

private enum class StartupStage {
    SPLASH,
    ONBOARDING,
    APP
}

@Composable
fun StartupFlow(
    isSignedIn: Boolean,
    onExit: () -> Unit,
    onboardingPages: List<@Composable (
        onNext: () -> Unit,
        onPageSelected: (Int) -> Unit
    ) -> Unit> = AvailableOnboardingPages,
    preferences: OnboardingPreferences = rememberOnboardingPreferences(),
    onSplashVisibilityChanged: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    var stage by rememberSaveable { mutableStateOf(StartupStage.SPLASH) }
    var pageIndex by rememberSaveable { mutableStateOf(0) }
    val currentIsSignedIn by rememberUpdatedState(isSignedIn)

    SideEffect {
        onSplashVisibilityChanged(stage == StartupStage.SPLASH)
    }

    BackHandler(enabled = stage != StartupStage.APP) {
        if (stage == StartupStage.ONBOARDING && pageIndex > 0) {
            pageIndex--
        } else {
            onExit()
        }
    }

    when (stage) {
        StartupStage.SPLASH -> SplashScreen {
            if (stage == StartupStage.SPLASH) {
                stage = if (currentIsSignedIn || preferences.isCompleted || onboardingPages.isEmpty()) {
                    StartupStage.APP
                } else {
                    pageIndex = 0
                    StartupStage.ONBOARDING
                }
            }
        }
        StartupStage.ONBOARDING -> {
            val page = onboardingPages.getOrNull(pageIndex)
            if (page == null) {
                LaunchedEffect(Unit) { stage = StartupStage.APP }
            } else {
                val renderedPageIndex = pageIndex
                page({
                    // Ignore an extra tap delivered before the next page is rendered.
                    if (stage == StartupStage.ONBOARDING && pageIndex == renderedPageIndex) {
                        if (pageIndex < onboardingPages.lastIndex) {
                            pageIndex++
                        } else {
                            preferences.markCompleted()
                            stage = StartupStage.APP
                        }
                    }
                }, { selectedPage ->
                    if (stage == StartupStage.ONBOARDING && selectedPage in onboardingPages.indices) {
                        pageIndex = selectedPage
                    }
                })
            }
        }
        StartupStage.APP -> content()
    }
}

@Composable
private fun rememberOnboardingPreferences(): OnboardingPreferences {
    val context = LocalContext.current.applicationContext
    return remember(context) { OnboardingPreferences(context) }
}
