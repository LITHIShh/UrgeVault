package com.betshield.app.util

import com.betshield.app.BetShieldApplication
import java.text.NumberFormat

object CurrencyFormatter {

    val currentSymbol: String
        get() = try {
            BetShieldApplication.instance.preferencesManager.selectedCurrencySymbol
        } catch (e: Exception) {
            "$"
        }

    val currentCode: String
        get() = try {
            BetShieldApplication.instance.preferencesManager.selectedCurrencyCode
        } catch (e: Exception) {
            "USD"
        }

    fun format(amount: Double): String {
        val sym = currentSymbol
        val rounded = Math.round(amount)
        val formatted = try {
            val nf = NumberFormat.getIntegerInstance()
            nf.format(rounded)
        } catch (e: Exception) {
            rounded.toString()
        }
        return "$sym$formatted"
    }

    fun format(amount: Number): String {
        return format(amount.toDouble())
    }
}
