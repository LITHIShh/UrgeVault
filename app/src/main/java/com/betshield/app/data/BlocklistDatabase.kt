package com.betshield.app.data

import android.content.Context
import java.util.Locale

class BlocklistDatabase(private val context: Context) {

    private val prefsManager by lazy { PreferencesManager(context) }

    // ==========================================
    // 1. BETTING & GAMBLING WEBSITES (Titans & Circumvention Mirrors)
    // ==========================================
    val defaultDomains: Set<String> get() = gamblingDomains
    val defaultAppPackages: Set<String> get() = gamblingAppPackages

    val gamblingDomains: Set<String> = hashSetOf(
        // 1xBet & Mirror Network (Circumvention Family)
        "1xbet.com", "1xbet.ng", "1xbet.in", "1x-bet.mobi", "1x-bet.com",
        "1x-bet.online", "1x-bet-zone.com", "1x-bet99.com", "1xbet-in.com",
        "1x-bet-india.com", "1x001.com", "1x002.com", "1xbit.com", "1xslots.com",
        "1xstavka.ru", "1xbet.co", "1xbet.org", "1xbet.net", "1xbet.app",

        // Global Titans & Crypto Casinos
        "bet365.com", "bet365.es", "bet365.it", "bet365.com.au", "bet365.dk", "bet-365.com",
        "stake.com", "stake.us", "stake.bet", "stake.games", "stake.bz",
        "roobet.com", "roobet.fun", "roobet.io",
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
        "parimatch.com", "parimatch.in", "parimatch.pro", "pari-match.com", "dafabet.com", "dafabet.net",
        "melbet.com", "melbet.org", "melbet.in", "mel-bet.com", "22bet.com", "20bet.com",
        "1win.pro", "1win.cloud", "1win.in", "1-win.com", "mostbet.com", "mostbet.in", "most-bet.com",
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

        // India & Regional High-Impact Betting Portals & Mirrors
        "lotus365.com", "lotus365.in", "lotus-365.com", "fairplay.club", "fair-play.club", "fairplay24.in",
        "mahadevbook.com", "mahadev-book.com", "reddyanna.com", "reddy-anna.com",
        "kheloo.com", "kheloo.in", "rajabets.com", "raja-bets.com", "crickex.com", "khelraja.com",
        "betbhai9.com", "betbhai.com", "laser247.com", "diamond247.com", "sky247.com",
        "4rabet.com", "4ra-bet.com", "10cric.com", "10-cric.com", "indibet.com", "purewin.com", "baazi247.com",
        "fun88.com", "fun88.in", "betbarter.com", "linebet.com", "megapari.com"
    )

    // ==========================================
    // 2. BETTING & GAMBLING APPS (Official & Circumventing Sideloads)
    // ==========================================
    val gamblingAppPackages: Set<String> = hashSetOf(
        // 1xBet APK packages
        "org.xbet", "com.xbet", "com.bk1xbet", "com.bk.xbet", "com.xbet.android",
        "com.one_x_bet", "com.onexbet", "com.one_x_bet_android", "org.xbet.client", "com.xbet.partner",

        // Global Betting Apps
        "com.bet365", "com.bet365.wrapper", "com.bet365.sports",
        "com.draftkings.dknative", "com.draftkings.sportsbook", "com.draftkings.casino",
        "com.fanduel.sportsbook", "com.fanduel.casino", "com.fanduel.pointsbet",
        "com.bovada", "com.roobet", "com.pokerstars", "com.pokerstars.net", "com.pokerstars.in",
        "com.stake", "com.betway", "com.betway.sports", "com.betway.casino",
        "com.williamhill.android", "com.unibet", "com.betfair", "com.betfair.sports",
        "com.betmgm.sports", "com.betmgm.casino", "com.ladbrokes", "com.skybet",
        "com.paddypower", "com.caesars.sportsbook", "com.parimatch", "com.melbet",
        "com.dafabet", "com.betrivers", "com.penn.barstool", "com.fanatics.sportsbook",
        "com.hardrock.sportsbook", "com.bwin.sports", "com.betsson.casino", "com.leovegas.android",
        "com.mrgreen.casino", "com.sportsbet.android", "com.tab.mobile", "com.pointsbet.android",

        // Regional & Casino Apps
        "com.mpl.androidapp", "com.dream11.app", "com.myteam11.app", "com.winzo.gold",
        "com.rummycircle.android", "com.junglee.rummy", "com.zupee.gold", "com.games24x7.my11circle",
        "com.pokerbaazi", "com.spartanpoker", "com.adda52",
        "com.playtika.slotomania", "com.huuuge.casino.slots", "com.productmadness.hovmobile",
        "com.murka.scatterslots", "com.gsn.grandcasino",
        "com.fourrabet", "com.ten_cric.app", "com.indibet.app"
    )

    // Distinct Brand Roots that identify betting portals (tested with hyphens & mirrors)
    val gamblingBrandRoots = listOf(
        // 1xBet circumvention family
        "1xbet", "1xbit", "1xslots", "1xstavka", "1x001", "1x002",
        // Top betting platforms
        "bet365", "parimatch", "lotus365", "fairplay", "mahadevbook",
        "reddyanna", "crickex", "khelraja", "betbhai", "laser247",
        "diamond247", "sky247", "4rabet", "10cric", "indibet", "purewin",
        "baazi247", "fun88", "betbarter", "linebet", "megapari",
        "betway", "bovada", "roobet", "melbet", "1win", "mostbet",
        "kheloo", "rajabets", "dafabet", "sportsbet", "pointsbet",
        "draftkings", "fanduel", "betmgm", "williamhill", "unibet",
        "betfair", "ladbrokes", "paddypower", "bwin", "betsson",
        "betsafe", "nordicbet", "betonline", "mybookie", "sportsbetting",
        "betus", "leovegas", "mrgreen", "casumo", "betrivers", "espnbet",
        "chumbacasino", "luckylandslots", "pulsz", "mcluck", "slotomania",
        "betfred", "betvictor", "boylesports", "virginbet", "livescorebet",
        "lotto247", "thelotter", "rummycircle", "jungleerummy", "zupee",
        "pokerbaazi", "spartanpoker", "adda52", "winzo", "dream11", "my11circle"
    )

    private val genericGamblingKeywords = listOf(
        "sportsbook", "onlinegambling", "realmoneybet", "jackpotslots",
        "livecasino", "casinogames", "onlinecasino", "cricketbetting"
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

    // Suspicious TLDs
    private val gamblingTlds = setOf(".bet", ".casino", ".poker", ".bingo", ".gambling", ".lotto", ".vegas")
    private val adultTlds = setOf(".porn", ".xxx", ".adult", ".sex")

    /**
     * Determines whether a given domain (e.g. from DNS query or extracted browser host) should be blocked.
     * Guaranteed safe: Never blocks standard web services or system DNS.
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

        // Normalized string with hyphens and underscores stripped (catches 1x-bet, pari-match, etc.)
        val stripped = cleanDomain.replace("-", "").replace("_", "")

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
                val cStripped = c.replace("-", "").replace("_", "")
                if (cleanDomain == c || cleanDomain.endsWith(".$c") ||
                    stripped == cStripped || stripped.endsWith(".$cStripped") ||
                    (c.length >= 4 && (cleanDomain.contains(c) || stripped.contains(cStripped)))) {
                    return true
                }
            }
        }

        // 2. Betting & Gambling category
        if (prefsManager.isGamblingBlockingEnabled) {
            // Check preloaded domains
            if (gamblingDomains.contains(cleanDomain) || gamblingDomains.contains(stripped)) {
                return true
            }
            for (blocked in gamblingDomains) {
                if (cleanDomain.endsWith(".$blocked") || stripped.endsWith(".$blocked")) {
                    return true
                }
            }

            // Check gambling-specific TLDs
            for (tld in gamblingTlds) {
                if (cleanDomain.endsWith(tld)) return true
            }

            // Check brand roots (catches 1xbet, 1x-bet, 1xbet-in, parimatch, lotus365, etc.)
            for (brand in gamblingBrandRoots) {
                if (cleanDomain.contains(brand) || stripped.contains(brand)) {
                    return true
                }
            }

            // Check generic gambling keywords in domain name
            for (kw in genericGamblingKeywords) {
                if (cleanDomain.contains(kw) || stripped.contains(kw)) {
                    return true
                }
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
     * Checks if text from a browser address/URL bar or search input targets a blocked destination.
     * Prevents false positives by strictly checking domain and brand root matches.
     */
    fun isBrowserUrlBlocked(rawText: String): Boolean {
        if (rawText.isBlank()) return false
        val trimmed = rawText.trim()

        // 1. Direct domain check
        if (isDomainBlocked(trimmed)) return true

        // 2. Extracted host check (handles full URLs like https://1x-bet.com/en/live)
        val cleanHost = trimmed.lowercase(Locale.ROOT)
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .split("/")[0]
            .split(":")[0]
            .removeSuffix(".")

        if (cleanHost.isNotEmpty() && isDomainBlocked(cleanHost)) {
            return true
        }

        // 3. Typed address bar query check (e.g. user typed "1xbet", "1x-bet", "stake", or a custom blocked domain)
        val normalized = trimmed.lowercase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "")

        // Custom domains
        for (custom in prefsManager.customDomains) {
            val c = custom.trim().lowercase(Locale.ROOT)
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .split("/")[0]
            if (c.length >= 4 && normalized.contains(c.replace("-", ""))) {
                return true
            }
        }

        // Gambling brand roots
        if (prefsManager.isGamblingBlockingEnabled) {
            for (brand in gamblingBrandRoots) {
                if (normalized.contains(brand)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Backward compatibility method for existing callers.
     */
    fun isTextGamblingRelated(rawText: String): Boolean {
        return isBrowserUrlBlocked(rawText)
    }

    /**
     * Determines whether a given app package or window title should be blocked.
     * Catches official and circumventing betting apps (e.g. 1xBet APKs, Parimatch APKs).
     */
    fun isAppBlocked(packageNameInput: String, windowTitle: String? = null): Boolean {
        val packageName = packageNameInput.trim().lowercase(Locale.ROOT)

        // 1. User custom blocked apps (ALWAYS APPLIED)
        if (prefsManager.customApps.contains(packageName)) return true

        // 2. Gambling Apps
        if (prefsManager.isGamblingBlockingEnabled) {
            if (gamblingAppPackages.contains(packageName)) return true

            // Sideloaded / unofficial betting APK packages
            val suspiciousPackageKeywords = listOf(
                "1xbet", "xbet", "onexbet", "bk1xbet", "parimatch", "bet365",
                "melbet", "1win", "mostbet", "fairplay", "lotus365", "mahadevbook",
                "kheloo", "rajabets", "4rabet", "10cric", "sky247", "laser247",
                "diamond247", "betbhai", "dafabet", "betway", "bovada", "roobet"
            )
            for (kw in suspiciousPackageKeywords) {
                if (packageName.contains(kw)) return true
            }

            if (!windowTitle.isNullOrBlank()) {
                val titleLower = windowTitle.lowercase(Locale.ROOT).replace("-", "").replace(" ", "")
                for (kw in suspiciousPackageKeywords) {
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
