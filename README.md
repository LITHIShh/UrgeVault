# UrgeVault — Protect Your Progress (Betting · Alcohol · Smoking)

**UrgeVault** is an Android-first, offline-first recovery companion and habit-change sanctuary. It combines a compassionate crisis-response toolkit (**Urge SOS**), transparent financial and time reclamation (**Money & Time Vault**), milestone-driven growth without shame (**Progress Garden**), and device-level hardware protection (**Vault Shield**).

---

## 🌟 Core Architecture & Feature Pillars

### 1. Urge SOS (Guided Crisis Intervention)
- **1-Tap Accessibility:** Prominently featured on the main dashboard for immediate craving response.
- **Urge Intensity Scale (1–10):** Real-time subjective intensity tracking to reflect neurochemical craving waves.
- **60-Second Grounding Exercise:** Interactive 4-7-8 breathing circle animation (4s Inhale, 7s Hold, 8s Exhale) that stimulates parasympathetic vagal tone and restores prefrontal cortex control.
- **15-Minute Craving Delay Timer:** Craving spikes peak and decline within 15 minutes. The timer holds the pause.
- **Why I Started:** Dynamic personal motivations for the specific habit to reinforce identity-level commitment.
- **Support & Crisis Line:** 1-tap ally speed dial and 24/7 confidential helplines (1-800-522-4700 / Tele-MANAS 14416).
- **Strictly Ad-Free:** Zero ads, zero notifications, zero forced navigation during Urge SOS flows.

### 2. Money & Time Vault (Tangible Reclamation)
Transparent calculations based on user baselines:
$$\text{Estimated Money Protected} = (\text{Baseline spend per day} \times \text{eligible days}) - \text{recorded lapses}$$
$$\text{Estimated Time Reclaimed} = \text{Baseline minutes per day} \times \text{eligible days}$$
- **Multi-Habit Support:**
  - **Betting & Gambling:** Tracks net money protected, bets avoided, and hours salvaged from chasing cycles.
  - **Alcohol:** Tracks money avoided, standard drinks avoided, and health milestone reclamation.
  - **Smoking & Nicotine:** Tracks money avoided, cigarettes not smoked, and lung stamina indicators.
- **Savings Goals:** Visual progress bars for target allocations (Emergency Fund, Weekend Trip, New Tech/Camera).

### 3. Progress Sanctuary & Reflection
- **Progress Garden:** Visual living environment that evolves through 5 stages based on check-in consistency and resilience:
  - 🌱 *Planted Seed* → 🌿 *Emerging Sprout* → 🪴 *Growing Sapling* → 🌳 *Resilient Grove* → 🌸 *Blooming Sanctuary*
- **Milestone Badges:** First Step, 3-Day Breakthrough, 1 Week Clean, 2 Weeks Grounded, 1 Month Milestone, 60 Days Resilience, Season of Transformation.
- **Compassionate Relapse Plan:** Lapses never erase historical progress. Slip-ups are treated as data, allowing net financial adjustments without shame.
- **Trigger & Coping Insights:** Aggregates top craving triggers (e.g., evening boredom, work stress) and most effective tools.

### 4. Vault Shield (Device-Level Trigger Protection)
- **Local DNS Sinkhole VPN:** Port 53 UDP interception blocking 500+ top gambling and betting domains at `0.0.0.0` with family-safe Cloudflare DNS upstream.
- **Accessibility Guardian:** Real-time foreground app and browser URL inspector (Chrome, Edge, Firefox, Brave) shutting down betting platforms immediately.
- **Promotional Notification Blocker:** Suppresses push notifications containing gambling keywords ("free bet", "spins", "deposit bonus").
- **Master PIN / Password Anti-Bypass:** PBKDF2/SHA-256 hashed master lock guarding shield deactivation and system settings.
- **Boot Persistence:** `directBootAware` receiver restarting protection seamlessly across device reboots.

### 5. Ethical Monetization (AdMob)
- **Non-Intrusive Bottom Banner:** Fixed neatly at the bottom of the screen away from recovery tools.
- **In-App Real Ad Unit Setup:** Easily switch between Google official test IDs and your live AdMob Ad Unit IDs right inside the app settings.

---

## 🔒 Privacy & Health Notice
- **100% Offline-First:** No accounts, no cloud sync, no tracking. All data is encrypted and stored locally in app sandboxed storage.
- **Medical Boundary:** Alcohol withdrawal can be dangerous for individuals with severe physical alcohol dependence. UrgeVault includes prominent medical notices guiding users to professional clinical consultation.

---

## 📦 Building APK & Play Store AAB

UrgeVault includes an automated GitHub Actions CI/CD pipeline:
1. Push the code to GitHub (`git push origin main` or double-click `push_to_github.bat`).
2. GitHub Actions runs `.github/workflows/main.yml`:
   - Builds debug APK (`app-debug.apk`)
   - Builds signed release bundle (`app-release.aab`) ready for Google Play Console upload.
3. Download the built APK or AAB under the **Actions** tab on your GitHub repository (https://github.com/LITHIShh/UrgeVault/actions)!
