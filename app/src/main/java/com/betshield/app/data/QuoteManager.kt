package com.betshield.app.data

import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.util.Random

data class RecoveryQuote(
    val quote: String,
    val author: String,
    val category: String
)

object QuoteManager {

    val QUOTES = listOf(
        RecoveryQuote(
            "You have power over your mind - not outside events. Realize this, and you will find strength.",
            "Marcus Aurelius",
            "Stoic Wisdom"
        ),
        RecoveryQuote(
            "Every action you take is a vote for the person you wish to become.",
            "James Clear",
            "Identity Shift"
        ),
        RecoveryQuote(
            "Between stimulus and response there is a space. In that space is our power to choose our response.",
            "Viktor E. Frankl",
            "Mindful Pause"
        ),
        RecoveryQuote(
            "He who conquers himself is the mightiest warrior.",
            "Confucius",
            "Self Mastery"
        ),
        RecoveryQuote(
            "The secret of change is to focus all of your energy not on fighting the old, but on building the new.",
            "Socrates",
            "Neuroplasticity"
        ),
        RecoveryQuote(
            "Cravings are like ocean waves. You cannot stop them from coming, but you can learn to surf them.",
            "Jon Kabat-Zinn",
            "Urge Surfing"
        ),
        RecoveryQuote(
            "Freedom is not the absence of desires, but the mastery of choice.",
            "Epictetus",
            "Stoic Freedom"
        ),
        RecoveryQuote(
            "A lapse is not a collapse unless you choose to quit. Every moment is a clean slate.",
            "Recovery Principle",
            "Compassion"
        ),
        RecoveryQuote(
            "Your impulse promises dopamine now, but costs your peace later. Protect your peace.",
            "UrgeVault Wisdom",
            "Clarity"
        ),
        RecoveryQuote(
            "Small disciplines repeated with consistency every day lead to great achievements gained slowly over time.",
            "John C. Maxwell",
            "Consistency"
        ),
        RecoveryQuote(
            "Do not spoil what you have by desiring what you have not.",
            "Epicurus",
            "Gratitude"
        ),
        RecoveryQuote(
            "The craving will peak and dissolve within 15 minutes. All you need is patience for this breath.",
            "Mindfulness Insight",
            "Brain Science"
        )
    )

    fun getTodayQuote(): RecoveryQuote {
        val dayIndex = (LocalDate.now().toEpochDay() % QUOTES.size).toInt()
        val safeIndex = if (dayIndex < 0) dayIndex + QUOTES.size else dayIndex
        return QUOTES[safeIndex]
    }

    fun getRandomQuote(exclude: RecoveryQuote? = null): RecoveryQuote {
        val filtered = QUOTES.filter { it.quote != exclude?.quote }
        return if (filtered.isNotEmpty()) {
            filtered[Random().nextInt(filtered.size)]
        } else {
            QUOTES[0]
        }
    }

    fun shareQuote(context: Context, quote: RecoveryQuote) {
        val shareText = "\"${quote.quote}\"\n— ${quote.author} (${quote.category})\n\nProtected with UrgeVault 🛡️"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Daily Recovery Wisdom")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Inspiration"))
    }
}
