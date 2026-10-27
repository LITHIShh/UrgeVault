package com.betshield.app.util

import androidx.appcompat.app.AppCompatDelegate
import com.betshield.app.data.PreferencesManager

object ThemeHelper {

    fun applyTheme(mode: String) {
        when (mode) {
            PreferencesManager.THEME_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            PreferencesManager.THEME_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else -> {
                // Auto / Time-based shift
                if (TimeThemeManager.isNightShiftActive()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }
    }

    fun isDarkActive(mode: String): Boolean {
        return when (mode) {
            PreferencesManager.THEME_LIGHT -> false
            PreferencesManager.THEME_DARK -> true
            else -> TimeThemeManager.isNightShiftActive()
        }
    }
}
