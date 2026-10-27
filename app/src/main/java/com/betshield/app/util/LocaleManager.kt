package com.betshield.app.util

import android.app.Activity
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import java.util.Locale

data class AppLocaleOption(
    val languageTag: String,
    val displayName: String,
    val nativeName: String,
    val currencySymbol: String,
    val currencyCode: String,
    val flagEmoji: String
) {
    val fullLabel: String
        get() = "$flagEmoji $displayName ($nativeName) • $currencySymbol $currencyCode"
}

object LocaleManager {

    val SUPPORTED_OPTIONS = listOf(
        AppLocaleOption("en-IN", "English (India)", "English", "₹", "INR", "🇮🇳"),
        AppLocaleOption("en-GB", "English (UK)", "English", "£", "GBP", "🇬🇧"),
        AppLocaleOption("en-US", "English (US / Global)", "English", "$", "USD", "🇺🇸"),
        AppLocaleOption("en-CA", "English (Canada)", "English", "C$", "CAD", "🇨🇦"),
        AppLocaleOption("en-AU", "English (Australia)", "English", "A$", "AUD", "🇦🇺"),
        AppLocaleOption("hi", "Hindi", "हिन्दी", "₹", "INR", "🇮🇳"),
        AppLocaleOption("es", "Spanish", "Español", "€", "EUR", "🇪🇸"),
        AppLocaleOption("fr", "French", "Français", "€", "EUR", "🇫🇷"),
        AppLocaleOption("de", "German", "Deutsch", "€", "EUR", "🇩🇪"),
        AppLocaleOption("pt", "Portuguese", "Português", "R$", "BRL", "🇧🇷"),
        AppLocaleOption("ja", "Japanese", "日本語", "¥", "JPY", "🇯🇵"),
        AppLocaleOption("ar", "Arabic", "العربية", "$", "USD", "🇸🇦")
    )

    fun getDetectedOption(): AppLocaleOption {
        val loc = Locale.getDefault()
        val country = (loc.country ?: "").uppercase()
        val lang = (loc.language ?: "").lowercase()

        if (lang == "hi") {
            return SUPPORTED_OPTIONS.first { it.languageTag == "hi" }
        }
        return when (country) {
            "IN" -> SUPPORTED_OPTIONS.first { it.languageTag == "en-IN" }
            "GB", "UK" -> SUPPORTED_OPTIONS.first { it.languageTag == "en-GB" }
            "CA" -> SUPPORTED_OPTIONS.first { it.languageTag == "en-CA" }
            "AU", "NZ" -> SUPPORTED_OPTIONS.first { it.languageTag == "en-AU" }
            "ES", "MX", "AR", "CO", "CL" -> SUPPORTED_OPTIONS.first { it.languageTag == "es" }
            "FR", "BE" -> SUPPORTED_OPTIONS.first { it.languageTag == "fr" }
            "DE", "AT", "CH" -> SUPPORTED_OPTIONS.first { it.languageTag == "de" }
            "BR", "PT" -> SUPPORTED_OPTIONS.first { it.languageTag == "pt" }
            "JP" -> SUPPORTED_OPTIONS.first { it.languageTag == "ja" }
            "SA", "AE", "EG", "QA", "KW" -> SUPPORTED_OPTIONS.first { it.languageTag == "ar" }
            else -> SUPPORTED_OPTIONS.first { it.languageTag == "en-US" }
        }
    }

    fun getCurrentOption(): AppLocaleOption {
        val savedTag = BetShieldApplication.instance.preferencesManager.selectedLanguageTag
        return SUPPORTED_OPTIONS.firstOrNull { it.languageTag == savedTag } ?: getDetectedOption()
    }

    fun applyLocale(languageTag: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun showLanguageCurrencyDialog(
        activity: Activity,
        isFirstStart: Boolean = false,
        onSelected: () -> Unit = {}
    ) {
        val prefs = BetShieldApplication.instance.preferencesManager
        val initialOption = if (prefs.isRegionSetupCompleted) {
            getCurrentOption()
        } else {
            getDetectedOption()
        }

        var selectedIndex = SUPPORTED_OPTIONS.indexOfFirst { it.languageTag == initialOption.languageTag }
        if (selectedIndex < 0) selectedIndex = 0

        val itemLabels = SUPPORTED_OPTIONS.map { it.fullLabel }.toTypedArray()

        val builder = AlertDialog.Builder(activity, R.style.Theme_BetShield_Dialog)
            .setTitle(if (isFirstStart) "🌐 Select Region & Currency" else "🌐 Language & Currency")
            .setSingleChoiceItems(itemLabels, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(if (isFirstStart) "Confirm & Continue" else "Save") { dialog, _ ->
                val chosen = SUPPORTED_OPTIONS[selectedIndex]
                prefs.selectedLanguageTag = chosen.languageTag
                prefs.selectedCurrencySymbol = chosen.currencySymbol
                prefs.selectedCurrencyCode = chosen.currencyCode
                prefs.isRegionSetupCompleted = true

                applyLocale(chosen.languageTag)
                onSelected()
                dialog.dismiss()

                Toast.makeText(
                    activity,
                    "${chosen.displayName} • ${chosen.currencySymbol} ${chosen.currencyCode} set",
                    Toast.LENGTH_SHORT
                ).show()
            }

        if (isFirstStart) {
            builder.setCancelable(false)
        } else {
            builder.setNegativeButton("Cancel", null)
            builder.setNeutralButton("Currency Only") { _, _ ->
                showCustomCurrencyDialog(activity, onSelected)
            }
        }

        builder.show()
    }

    fun showCustomCurrencyDialog(activity: Activity, onSelected: () -> Unit) {
        val prefs = BetShieldApplication.instance.preferencesManager
        val currencies = arrayOf(
            "₹ - Indian Rupee (INR)",
            "$ - US Dollar (USD)",
            "£ - British Pound (GBP)",
            "€ - Euro (EUR)",
            "C$ - Canadian Dollar (CAD)",
            "A$ - Australian Dollar (AUD)",
            "R$ - Brazilian Real (BRL)",
            "¥ - Japanese Yen (JPY)",
            "Custom Symbol..."
        )

        AlertDialog.Builder(activity, R.style.Theme_BetShield_Dialog)
            .setTitle("Select Currency")
            .setItems(currencies) { _, which ->
                when (which) {
                    0 -> setCurrency(prefs, "₹", "INR", onSelected)
                    1 -> setCurrency(prefs, "$", "USD", onSelected)
                    2 -> setCurrency(prefs, "£", "GBP", onSelected)
                    3 -> setCurrency(prefs, "€", "EUR", onSelected)
                    4 -> setCurrency(prefs, "C$", "CAD", onSelected)
                    5 -> setCurrency(prefs, "A$", "AUD", onSelected)
                    6 -> setCurrency(prefs, "R$", "BRL", onSelected)
                    7 -> setCurrency(prefs, "¥", "JPY", onSelected)
                    8 -> promptCustomCurrencySymbol(activity, onSelected)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setCurrency(
        prefs: com.betshield.app.data.PreferencesManager,
        symbol: String,
        code: String,
        onSelected: () -> Unit
    ) {
        prefs.selectedCurrencySymbol = symbol
        prefs.selectedCurrencyCode = code
        onSelected()
    }

    private fun promptCustomCurrencySymbol(activity: Activity, onSelected: () -> Unit) {
        val prefs = BetShieldApplication.instance.preferencesManager
        val input = EditText(activity).apply {
            hint = "Symbol (e.g. ₣, kr, ₪)"
            setText(prefs.selectedCurrencySymbol)
            setSelection(text.length)
        }

        AlertDialog.Builder(activity, R.style.Theme_BetShield_Dialog)
            .setTitle("Enter Currency Symbol")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val symbol = input.text.toString().trim()
                if (symbol.isNotEmpty()) {
                    prefs.selectedCurrencySymbol = symbol
                    onSelected()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
