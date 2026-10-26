package com.betshield.app.data

import android.content.Context
import java.util.Locale

class BlocklistDatabase(private val context: Context) {

    private val prefsManager by lazy { PreferencesManager(context) }

    // Precompiled set of top gambling domains
    val defaultDomains: Set<String> = hashSetOf(
        // Global Giants
        "bet365.com", "bet365.es", "bet365.it", "bet365.com.au", "bet365.dk",
        "stake.com", "stake.us", "stake.bet", "stake.games",
        "roobet.com", "roobet.fun", "roobet.io",
        "1xbet.com", "1xbet.ng", "1xbet.in", "1x-bet.mobi",
        "bovada.lv", "bovada.com",
        "draftkings.com", "sportsbook.draftkings.com", "casino.draftkings.com",
        "fanduel.com", "sportsbook.fanduel.com", "casino.fanduel.com",
        "betmgm.com", "sports.betmgm.com", "casino.betmgm.com",
        "caesars.com", "williamhill.com", "williamhill.es",
        "betway.com", "betway.co.za", "betway.es",
        "888casino.com", "888sport.com", "888poker.com", "888.com",
        "pokerstars.com", "pokerstars.eu", "pokerstars.uk", "pokerstars.net",
        "unibet.com", "unibet.co.uk", "unibet.fr",
        "betfair.com", "betfair.es", "betfair.it",
        "ladbrokes.com", "ladbrokes.com.au",
        "skybet.com", "skyvegas.com", "skypoker.com",
        "paddypower.com", "coral.co.uk", "bwin.com", "bwin.es",
        "betsson.com", "betsafe.com", "nordicbet.com",
        "betonline.ag", "mybookie.ag", "sportsbetting.ag",
        "ignitioncasino.eu", "ignitioncasino.com", "cafecasino.lv", "slots.lv",
        "spincasino.com", "jackpotcitycasino.com", "rubyfortune.com",
        "leovegas.com", "mrgreen.com", "rizk.com", "dunder.com",
        "betrivers.com", "playrivers.com", "sugarhouse.com",
        "espnbet.com", "fanatics.com/sportsbook", "hardrock.bet", "hardrocksportsbook.com",
        "parimatch.com", "parimatch.in", "dafabet.com",
        "melbet.com", "melbet.org", "22bet.com", "20bet.com",
        "1win.pro", "1win.cloud", "mostbet.com", "pin-up.casino", "pin-up.bet",
        "bet9ja.com", "sportybet.com", "betika.com", "hollywoodbets.net",
        "sportsbet.com.au", "tab.com.au", "neds.com.au", "pointsbet.com",
        "bitstarz.com", "7bitcasino.com", "mbitcasino.io", "cloudbet.com",
        "rollbit.com", "csgoroll.com", "csgoluck.com", "gamdom.com", "duelbits.com",
        "shuffle.com", "bc.game", "bcgame.com", "hypedrop.com",
        "chumbacasino.com", "luckylandslots.com", "pulsz.com", "funzpoints.com",
        "slotomania.com", "doubledowncasino.com", "heartofvegas.com", "huuugecasino.com",
        "betfred.com", "betvictor.com", "boylesports.com", "virginbet.com",
        "livescorebet.com", "grosvenorcasinos.com", "meccabingo.com",
        "foxybingo.com", "tombola.co.uk", "lotto247.com", "thelotter.com"
    )

    // Known betting app package names
    val defaultAppPackages: Set<String> = hashSetOf(
        "com.bet365", "com.bet365.wrapper", "com.bet365.sports",
        "com.draftkings.dknative", "com.draftkings.sportsbook", "com.draftkings.casino",
        "com.fanduel.sportsbook", "com.fanduel.casino", "com.fanduel.pointsbet",
        "com.bovada", "com.roobet", "com.pokerstars", "com.pokerstars.net",
        "com.stake", "com.betway", "com.betway.sports", "com.betway.casino",
        "com.williamhill.android", "com.unibet", "com.betfair", "com.betfair.sports",
        "com.betmgm.sports", "com.betmgm.casino", "com.ladbrokes", "com.skybet",
        "com.paddypower", "com.caesars.sportsbook", "com.parimatch", "com.melbet",
        "com.dafabet", "com.betrivers", "com.penn.barstool", "com.fanatics.sportsbook",
        "com.hardrock.sportsbook", "com.mpl.androidapp", "com.dream11.app",
        "com.myteam11.app", "com.winzo.gold", "com.rummycircle.android",
        "com.junglee.rummy", "com.zupee.gold", "com.games24x7.my11circle",
        "com.playtika.slotomania", "com.huuuge.casino.slots", "com.productmadness.hovmobile",
        "com.murka.scatterslots", "com.gsn.grandcasino", "com.bwin.sports",
        "com.betsson.casino", "com.leovegas.android", "com.mrgreen.casino",
        "com.sportsbet.android", "com.tab.mobile", "com.pointsbet.android"
    )

    // Suspicious gambling TLDs
    private val gamblingTlds = setOf(".bet", ".casino", ".poker", ".bingo", ".gambling", ".lotto", ".vegas")

    // Keywords in domains or app titles
    private val gamblingKeywords = listOf(
        "casino", "sportsbook", "betting", "blackjack", "roulette",
        "slotmachine", "jackpotslots", "realmoneybet", "onlinegambling"
    )

    /**
     * Determines whether a given domain (e.g. from DNS query) should be blocked.
     */
    fun isDomainBlocked(domainInput: String): Boolean {
        val domain = domainInput.trim().lowercase(Locale.ROOT).removeSuffix(".")

        // 1. Check user custom list (exact match or any subdomain)
        for (custom in prefsManager.customDomains) {
            if (domain == custom || domain.endsWith(".$custom")) {
                return true
            }
        }

        // 2. Check exact or subdomain match against curated list
        for (blocked in defaultDomains) {
            if (domain == blocked || domain.endsWith(".$blocked")) {
                return true
            }
        }

        // 3. Check gambling TLDs
        for (tld in gamblingTlds) {
            if (domain.endsWith(tld)) {
                return true
            }
        }

        // 4. Keyword heuristic check
        for (kw in gamblingKeywords) {
            if (domain.contains(kw)) {
                return true
            }
        }

        return false
    }

    /**
     * Determines whether a given app package or window title should be blocked.
     */
    fun isAppBlocked(packageNameInput: String, windowTitle: String? = null): Boolean {
        val packageName = packageNameInput.trim().lowercase(Locale.ROOT)

        // 1. User custom blocked apps
        if (prefsManager.customApps.contains(packageName)) return true

        // 2. Curated package list
        for (blockedPkg in defaultAppPackages) {
            if (packageName == blockedPkg || packageName.startsWith("$blockedPkg.")) {
                return true
            }
        }

        // 3. Check for keywords in package name
        for (kw in gamblingKeywords) {
            if (packageName.contains(kw)) {
                return true
            }
        }

        // 4. Check window title if available
        if (!windowTitle.isNullOrBlank()) {
            val titleLower = windowTitle.lowercase(Locale.ROOT)
            for (kw in gamblingKeywords) {
                if (titleLower.contains(kw)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Total count of blocked items in the database.
     */
    fun getTotalBlockedCount(): Int {
        return defaultDomains.size + defaultAppPackages.size + prefsManager.customDomains.size + prefsManager.customApps.size
    }
}
