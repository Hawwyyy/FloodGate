package com.example.floodgate.data.onboarding

import android.content.Context

class OnboardingPreferences(
    context: Context,
    preferencesName: String = "onboarding"
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        preferencesName,
        Context.MODE_PRIVATE
    )

    val isCompleted: Boolean
        get() = preferences.getInt(COMPLETED_VERSION_KEY, 0) >= CURRENT_ONBOARDING_VERSION

    fun markCompleted() {
        preferences.edit().putInt(COMPLETED_VERSION_KEY, CURRENT_ONBOARDING_VERSION).apply()
    }

    private companion object {
        // Version 2 adds the two missing pages, so users who only saw the original
        // one-page flow receive the complete introduction once.
        const val CURRENT_ONBOARDING_VERSION = 2
        const val COMPLETED_VERSION_KEY = "completed_version"
    }
}
