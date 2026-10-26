# BetShield - Gambling & Betting Blocker for Android

**BetShield** is a complete, self-contained Android application designed to protect users from gambling addiction by blocking gambling websites, betting applications, and preventing bypass attempts via a secure master password lock.

---

## Key Features

1. **Website Blocking (Local DNS Sinkhole VPN):**
   - Implements Android's `VpnService` to capture outbound DNS requests locally on the device (port 53 UDP).
   - Zero external proxy lag: checks queried domains against a database of 500+ top gambling portals (Bet365, Stake, Roobet, DraftKings, FanDuel, Bovada, 888, 1xBet, etc.).
   - Drops/sinkholes betting domains immediately by synthesizing a `0.0.0.0` response.
   - Clean traffic is securely resolved via high-privacy upstream family DNS (`1.1.1.3` / Cloudflare Family, blocking adult/gambling malware).

2. **App Blocking (Accessibility Guardian):**
   - Implements `AccessibilityService` (`BetBlockerAccessibilityService`) to detect foreground window switches in real time.
   - Intercepts known gambling apps (60+ top betting and casino packages) and keyword heuristics (`casino`, `sportsbook`, `slots`, `blackjack`, etc.).
   - Automatically kicks the user back to the Home screen and displays a high-priority, motivational `BlockedActivity` screen.

3. **Master Password Lock & Anti-Bypass:**
   - Hashed using **PBKDF2/SHA-256** with a cryptographic 16-byte salt and 10,000 iterations.
   - **Protection Toggle Lock:** Users cannot turn off the shield without entering the master password.
   - **Settings Lock:** Accessibility guardian detects attempts to open Android Settings (`com.android.settings`) -> BetShield -> "Force Stop" or "Uninstall" and locks access until authorized.
   - **Device Admin Receiver:** Integrates `DevicePolicyManager` to prevent instant uninstallation.

4. **Motivational Intervention & Crisis Support:**
   - Full-screen intervention overlay (`BlockedActivity`) providing inspirational self-reflection quotes and instant one-tap emergency calling to 24/7 gambling helplines (e.g. `1-800-GAMBLER`).

5. **Custom App & Website Blocker:**
   - Block **ANY installed application** on the phone (Instagram, TikTok, YouTube, Reddit, Games, etc.) via an interactive app selector with icons.
   - Block **ANY custom domain** (subdomains automatically protected).
   - Unblocking custom apps or websites requires the Master Password.

6. **AdMob Monetization (100% Free for Users, Passive Revenue for You):**
   - Integrates Google Mobile Ads SDK (`play-services-ads`).
   - **Adaptive Banner Ads:** Displayed unobtrusively at the bottom of the dashboard and blocker screen.
   - **Interstitial Ads:** Non-intrusive full-screen ads on rule edits or screen transitions.
   - **Rewarded Video Ads:** Users can watch a sponsor video ad to earn an optional "10-Minute Emergency Access", generating high-eCPM rewarded revenue while keeping the app completely free!
   - Configured with official Google sample test ad units out-of-the-box (no account ban risk during testing).

---

## Project Structure

```
BetShield/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/betshield/app/
│   │   │   ├── BetShieldApplication.kt
│   │   │   ├── data/
│   │   │   │   ├── BlocklistDatabase.kt       # 500+ domains, 60+ apps, keyword heuristics
│   │   │   │   ├── PasswordManager.kt         # Salted SHA-256 password hash & session lock
│   │   │   │   └── PreferencesManager.kt      # Configuration & interception stats
│   │   │   ├── service/
│   │   │   │   ├── BetShieldVpnService.kt     # Local DNS sinkhole tunnel
│   │   │   │   ├── BetBlockerAccessibilityService.kt # Real-time app blocker & anti-tamper
│   │   │   │   └── DnsPacketParser.kt         # IPv4/UDP/DNS packet parser & response generator
│   │   │   ├── receiver/
│   │   │   │   ├── BootReceiver.kt            # Auto-restart on device reboot
│   │   │   │   └── BetShieldDeviceAdminReceiver.kt # Device Admin uninstall protection
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt            # Dashboard, master switch with password lock
│   │   │       ├── BlockedActivity.kt         # Fullscreen blocking overlay
│   │   │       ├── PasswordDialog.kt          # PIN/Password setup & verification dialog
│   │   │       ├── PasswordUnlockActivity.kt  # Standalone unlock activity
│   │   │       ├── BlocklistActivity.kt       # Searchable list & custom rule manager
│   │   │       └── SetupPermissionsActivity.kt# Guided permissions onboarding
│   │   └── res/
│   │       ├── layout/                        # Material 3 responsive layouts
│   │       ├── values/                        # Dark slate & emerald palette, strings
│   │       └── xml/                           # Accessibility config & Device Admin spec
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## How to Build and Run in Android Studio

1. **Open the Project:**
   - Launch **Android Studio**.
   - Select **Open an existing Android Studio Project**.
   - Choose the directory: `C:\Users\Lithish r\.gemini\antigravity\scratch\BetShield`.

2. **Sync and Build:**
   - Allow Gradle to sync dependencies (AndroidX Core, Material 3, Coroutines).
   - Click **Run** (`Shift + F10`) or select **Build > Build Bundle(s) / APK(s) > Build APK(s)** to generate the `.apk`.

3. **First-Time Setup on Device / Emulator:**
   - **Step 1:** On launch, set your **Master Password** (e.g. 4-8 digit PIN).
   - **Step 2:** Tap **Permission & Anti-Bypass Setup**:
     - Enable **Web Filter**: Approve Android's VPN connection prompt.
     - Enable **App Guardian**: Enable BetShield under Android Accessibility settings.
     - Activate **Device Admin**: Protects the app from uninstallation without the password.
   - **Step 3:** Try visiting a betting website (e.g. `bet365.com` or `stake.com`) in any browser or launching a gambling app to see the instant blocking in action!
