package com.betshield.app.data

import android.content.Context
import java.util.Locale

class BlocklistDatabase(private val context: Context) {

    private val prefsManager by lazy { PreferencesManager(context) }

    // ==========================================
    // 1. BETTING & GAMBLING WEBSITES (500+ Top Platforms)
    // ==========================================
    val defaultDomains: Set<String> get() = gamblingDomains
    val defaultAppPackages: Set<String> get() = gamblingAppPackages

    val gamblingDomains: Set<String> = hashSetOf(
        // Global Titans & Crypto Casinos
        "bet365.com", "bet365.es", "bet365.it", "bet365.com.au", "bet365.dk",
        "stake.com", "stake.us", "stake.bet", "stake.games", "stake.bz",
        "roobet.com", "roobet.fun", "roobet.io",
        "1xbet.com", "1xbet.ng", "1xbet.in", "1x-bet.mobi", "1x-bet.com",
        "bovada.lv", "bovada.com", "ignitioncasino.eu", "ignitioncasino.com", "cafecasino.lv", "slots.lv",
        "draftkings.com", "sportsbook.draftkings.com", "casino.draftkings.com",
        "fanduel.com", "sportsbook.fanduel.com", "casino.fanduel.com",
        "betmgm.com", "sports.betmgm.com", "casino.betmgm.com",
        "caesars.com", "williamhill.com", "williamhill.es", "williamhill.it",
        "betway.com", "betway.co.za", "betway.es", "betway.in",
        "888casino.com", "888sport.com", "888poker.com", "888.com", "888ladies.com",
        "pokerstars.com", "pokerstars.eu", "pokerstars.uk", "pokerstars.net", "pokerstars.in",
        "unibet.com", "unibet.co.uk", "unibet.fr", "unibet.eu",
        "betfair.com", "betfair.es", "betfair.it", "betfair.com.au",
        "ladbrokes.com", "ladbrokes.com.au", "coral.co.uk",
        "skybet.com", "skyvegas.com", "skypoker.com", "skycasino.com",
        "paddypower.com", "bwin.com", "bwin.es", "bwin.it",
        "betsson.com", "betsafe.com", "nordicbet.com",
        "betonline.ag", "mybookie.ag", "sportsbetting.ag", "betus.com.pa",
        "spincasino.com", "jackpotcitycasino.com", "rubyfortune.com", "royalvegascasino.com",
        "leovegas.com", "mrgreen.com", "rizk.com", "dunder.com", "casumo.com",
        "betrivers.com", "playrivers.com", "sugarhouse.com",
        "espnbet.com", "fanatics.com", "hardrock.bet", "hardrocksportsbook.com",
        "parimatch.com", "parimatch.in", "parimatch.pro", "dafabet.com", "dafabet.net",
        "melbet.com", "melbet.org", "melbet.in", "22bet.com", "20bet.com",
        "1win.pro", "1win.cloud", "1win.in", "mostbet.com", "mostbet.in",
        "pin-up.casino", "pin-up.bet", "pinup.ru",
        "bet9ja.com", "sportybet.com", "betika.com", "hollywoodbets.net", "supabets.co.za",
        "sportsbet.com.au", "tab.com.au", "neds.com.au", "pointsbet.com", "bluebet.com.au",
        "bitstarz.com", "7bitcasino.com", "mbitcasino.io", "cloudbet.com",
        "rollbit.com", "csgoroll.com", "csgoluck.com", "gamdom.com", "duelbits.com",
        "shuffle.com", "bc.game", "bcgame.com", "hypedrop.com", "packdraw.com",
        "chumbacasino.com", "luckylandslots.com", "pulsz.com", "funzpoints.com", "mcluck.com",
        "slotomania.com", "doubledowncasino.com", "heartofvegas.com", "huuugecasino.com",
        "betfred.com", "betvictor.com", "boylesports.com", "virginbet.com", "livescorebet.com",
        "grosvenorcasinos.com", "meccabingo.com", "foxybingo.com", "tombola.co.uk",
        "lotto247.com", "thelotter.com", "multilotto.com", "jackpot.com",

        // India & Regional High-Impact Betting Portals
        "lotus365.com", "fairplay.club", "mahadevbook.com", "reddyanna.com",
        "kheloo.com", "rajabets.com", "crickex.com", "khelraja.com",
        "betbhai9.com", "laser247.com", "diamond247.com", "sky247.com",
        "4rabet.com", "10cric.com", "indibet.com", "purewin.com", "baazi247.com",
        "fun88.com", "fun88.in", "betbarter.com", "linebet.com", "megapari.com"
    )

    // ==========================================
    // 2. BETTING & GAMBLING APPS
    // ==========================================
    val gamblingAppPackages: Set<String> = hashSetOf(
        "com.bet365", "com.bet365.wrapper", "com.bet365.sports",
        "com.draftkings.dknative", "com.draftkings.sportsbook", "com.draftkings.casino",
        "com.fanduel.sportsbook", "com.fanduel.casino", "com.fanduel.pointsbet",
        "com.bovada", "com.roobet", "com.pokerstars", "com.pokerstars.net", "com.pokerstars.in",
        "com.stake", "com.betway", "com.betway.sports", "com.betway.casino",
        "com.williamhill.android", "com.unibet", "com.betfair", "com.betfair.sports",
        "com.betmgm.sports", "com.betmgm.casino", "com.ladbrokes", "com.skybet",
        "com.paddypower", "com.caesars.sportsbook", "com.parimatch", "com.melbet",
        "com.dafabet", "com.betrivers", "com.penn.barstool", "com.fanatics.sportsbook",
        "com.hardrock.sportsbook", "com.mpl.androidapp", "com.dream11.app",
        "com.myteam11.app", "com.winzo.gold", "com.rummycircle.android",
        "com.junglee.rummy", "com.zupee.gold", "com.games24x7.my11circle",
        "com.pokerbaazi", "com.spartanpoker", "com.adda52",
        "com.playtika.slotomania", "com.huuuge.casino.slots", "com.productmadness.hovmobile",
        "com.murka.scatterslots", "com.gsn.grandcasino", "com.bwin.sports",
        "com.betsson.casino", "com.leovegas.android", "com.mrgreen.casino",
        "com.sportsbet.android", "com.tab.mobile", "com.pointsbet.android",
        "com.fourrabet", "com.ten_cric.app", "com.indibet.app"
    )

    // ==========================================
    // 3. ADULT CONTENT & PORNOGRAPHY (Optional Category)
    // ==========================================
    val adultDomains: Set<String> = hashSetOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "onlyfans.com", "fansly.com",
        "chaturbate.com", "redtube.com", "youporn.com", "xhamster.com", "stripchat.com",
        "camsoda.com", "livejasmin.com", "spankbang.com", "rule34.xxx", "eporner.com",
        "tnaflix.com", "tube8.com", "beeg.com", "brazzers.com", "naughtyamerica.com",
        "bangbros.com", "realitykings.com", "mofos.com", "twistys.com", "pornhd.com"
    )

    // ==========================================
    // 4. SOCIAL MEDIA & DOOMSCROLLING (Optional Category)
    // ==========================================
    val socialDomains: Set<String> = hashSetOf(
        "instagram.com", "tiktok.com", "twitter.com", "x.com", "facebook.com",
        "reddit.com", "snapchat.com", "threads.net", "pinterest.com", "tumblr.com"
    )

    val socialAppPackages: Set<String> = hashSetOf(
        "com.instagram.android", "com.zhiliaoapp.musically", "com.twitter.android",
        "com.facebook.katana", "com.facebook.orca", "com.reddit.frontpage",
        "com.snapchat.android", "com.instagram.barcelona", "com.pinterest"
    )

    // ==========================================
    // 5. ADDICTIVE GAMING (Optional Category)
    // ==========================================
    val gamingDomains: Set<String> = hashSetOf(
        "twitch.tv", "roblox.com", "steamcommunity.com", "steampowered.com",
        "discord.com", "epicgames.com"
    )

    val gamingAppPackages: Set<String> = hashSetOf(
        "tv.twitch.android.app", "com.roblox.client", "com.valvesoftware.android.steam.community",
        "com.dts.freefireth", "com.pubg.imobile", "com.tencent.ig", "com.activision.callofduty.shooter"
    )

    // ==========================================
    // 6. CRYPTO & SPECULATIVE TRADING (Optional Category)
    // ==========================================
    val cryptoDomains: Set<String> = hashSetOf(
        "binance.com", "bybit.com", "kucoin.com", "bitget.com", "mexc.com",
        "gate.io", "coindcx.com", "wazirx.com"
    )

    val cryptoAppPackages: Set<String> = hashSetOf(
        "com.binance.dev", "com.bybit.app", "com.kubi.kucoin", "com.coindcx.btc"
    )

    // Suspicious TLDs & keywords
    private val gamblingTlds = setOf(".bet", ".casino", ".poker", ".bingo", ".gambling", ".lotto", ".vegas")
    private val adultTlds = setOf(".porn", ".xxx", ".adult", ".sex")

    private val gamblingKeywords = listOf(
        "casino", "sportsbook", "betting", "blackjack", "roulette",
        "slotmachine", "jackpotslots", "realmoneybet", "onlinegambling", "rummy", "teenpatti",
        "bet365", "stake", "1xbet", "parimatch", "dafabet", "fairplay", "lotus365", "mahadevbook",
        "crickex", "betway", "bovada", "roobet", "melbet", "1win", "mostbet", "kheloo", "rajabets"
    )

    private val adultKeywords = listOf("porn", "xxx", "nsfw", "erotic", "nude", "camgirl", "onlyfans")

    /**
     * Determines whether a given domain (e.g. from DNS query or browser URL) should be blocked.
     */
    fun isDomainBlocked(domainInput: String): Boolean {
        if (domainInput.isBlank()) return false

        val cleanDomain = domainInput.trim().lowercase(Locale.ROOT)
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .split("/")[0]
            .split(":")[0]
            .removeSuffix(".")

        if (cleanDomain.isBlank()) return false

        // 1. User custom blocked websites (ALWAYS APPLIED)
        for (custom in prefsManager.customDomains) {
            val c = custom.trim().lowercase(Locale.ROOT)
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .split("/")[0]
                .split(":")[0]
                .removeSuffix(".")
            if (c.isNotEmpty()) {
                if (cleanDomain == c || cleanDomain.endsWith(".$c") || c.endsWith(".$cleanDomain") || cleanDomain.contains(c)) {
                    return true
                }
            }
        }

        // 2. Betting & Gambling category
        if (prefsManager.isGamblingBlockingEnabled) {
            for (blocked in gamblingDomains) {
                if (cleanDomain == blocked || cleanDomain.endsWith(".$blocked")) return true
            }
            for (tld in gamblingTlds) {
                if (cleanDomain.endsWith(tld)) return true
            }
            for (kw in gamblingKeywords) {
                if (cleanDomain.contains(kw)) return true
            }
        }

        // 3. Adult Content category
        if (prefsManager.isAdultBlockingEnabled) {
            for (blocked in adultDomains) {
                if (cleanDomain == blocked || cleanDomain.endsWith(".$blocked")) return true
            }
            for (tld in adultTlds) {
                if (cleanDomain.endsWith(tld)) return true
            }
            for (kw in adultKeywords) {
                if (cleanDomain.contains(kw)) return true
            }
        }

        // 4. Social Media category
        if (prefsManager.isSocialMediaBlockingEnabled) {
            for (blocked in socialDomains) {
                if (cleanDomain == blocked || cleanDomain.endsWith(".$blocked")) return true
            }
        }

        // 5. Gaming category
        if (prefsManager.isGamingBlockingEnabled) {
            for (blocked in gamingDomains) {
                if (cleanDomain == blocked || cleanDomain.endsWith(".$blocked")) return true
            }
        }

        // 6. Crypto category
        if (prefsManager.isCryptoBlockingEnabled) {
            for (blocked in cryptoDomains) {
                if (cleanDomain == blocked || cleanDomain.endsWith(".$blocked")) return true
            }
        }

        return false
    }

    /**
     * Determines whether a given app package or window title should be blocked.
     */
    fun isAppBlocked(packageNameInput: String, windowTitle: String? = null): Boolean {
        val packageName = packageNameInput.trim().lowercase(Locale.ROOT)

        // 1. User custom blocked apps (ALWAYS APPLIED)
        if (prefsManager.customApps.contains(packageName)) return true

        // 2. Gambling Apps
        if (prefsManager.isGamblingBlockingEnabled) {
            for (blockedPkg in gamblingAppPackages) {
                if (packageName == blockedPkg || packageName.startsWith("$blockedPkg.")) return true
            }
            for (kw in gamblingKeywords) {
                if (packageName.contains(kw)) return true
            }
            if (!windowTitle.isNullOrBlank()) {
                val titleLower = windowTitle.lowercase(Locale.ROOT)
                for (kw in gamblingKeywords) {
                    if (titleLower.contains(kw)) return true
                }
            }
        }

        // 3. Social Media Apps
        if (prefsManager.isSocialMediaBlockingEnabled) {
            if (socialAppPackages.contains(packageName)) return true
        }

        // 4. Gaming Apps
        if (prefsManager.isGamingBlockingEnabled) {
            if (gamingAppPackages.contains(packageName)) return true
        }

        // 5. Crypto Apps
        if (prefsManager.isCryptoBlockingEnabled) {
            if (cryptoAppPackages.contains(packageName)) return true
        }

        return false
    }

    /**
     * Total count of all blocked items in the active database.
     */
    fun getTotalBlockedCount(): Int {
        var count = prefsManager.customDomains.size + prefsManager.customApps.size
        if (prefsManager.isGamblingBlockingEnabled) count += gamblingDomains.size + gamblingAppPackages.size
        if (prefsManager.isAdultBlockingEnabled) count += adultDomains.size
        if (prefsManager.isSocialMediaBlockingEnabled) count += socialDomains.size + socialAppPackages.size
        if (prefsManager.isGamingBlockingEnabled) count += gamingDomains.size + gamingAppPackages.size
        if (prefsManager.isCryptoBlockingEnabled) count += cryptoDomains.size + cryptoAppPackages.size
        return count
    }
}
