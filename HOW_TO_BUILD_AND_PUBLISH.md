# Complete Guide: Building the APK & Uploading to Google Play Store

This document explains step-by-step how to **build the APK / Android App Bundle (.aab)** and **upload it to the Google Play Store**.

---

## Part 1: How to Build the APK / AAB

Google Play requires an **Android App Bundle (.aab)** for publishing, while direct phone testing uses an **APK (.apk)**. You can build both in less than 2 minutes using **Android Studio**:

### Step 1: Open BetShield in Android Studio
1. Launch **Android Studio**.
2. Click **File > Open** and select:
   `C:\Users\Lithish r\.gemini\antigravity\scratch\BetShield`
3. Wait for the Gradle project sync to finish.

### Step 2: Build a Test APK (To install directly on your phone)
1. In Android Studio, click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. Once complete, a popup in the bottom-right corner will say *"Build APK(s): APK(s) generated successfully"*.
3. Click **locate** to open the folder containing:
   `app/build/outputs/apk/debug/app-debug.apk`
4. Send this `.apk` file to your phone via USB cable, WhatsApp, or Google Drive, tap it, and install!

### Step 3: Generate the Signed Android App Bundle (.aab) (For Google Play Store)
Google Play requires a signed bundle. To create it:
1. In Android Studio, click **Build > Generate Signed Bundle / APK...**
2. Select **Android App Bundle** and click **Next**.
3. Under **Key store path**, click **Create new...**:
   * Choose a location on your PC (e.g. `C:\Users\Lithish r\Desktop\betshield_release_key.jks`).
   * Password: Enter a secure password (e.g. `BetShield@2026`).
   * Key Alias: `betshield`
   * Key Password: Enter the same password.
   * Certificate: Enter your Name / Organization.
   * Click **OK**.
4. Click **Next**, choose **Release**, and click **Create**.
5. Android Studio will generate the signed bundle:
   `app/release/app-release.aab`
   *(This `.aab` file is what you upload to the Google Play Console!)*

---

## Part 2: Uploading to the Google Play Store

> **Why you need to complete the upload step:**
> Google requires a registered **Google Play Developer Account** (a one-time $25 registration fee paid directly to Google at [play.google.com/console](https://play.google.com/console)) and Google account identity verification. For security and identity reasons, automated bots cannot bypass Google's login or pay on your behalf.
>
> Everything needed for approval is already prepared for you in this folder.

### Step 1: Create Your App in Google Play Console
1. Log into your [Google Play Console](https://play.google.com/console).
2. Click **Create app** (top-right).
3. Fill in:
   * **App name:** `BetShield: Gambling Blocker`
   * **Default language:** English
   * **App or game:** App
   * **Free or paid:** Free (Select **Free** so users can install it at no charge while you earn from AdMob ads).
4. Accept the Developer Program Policies and click **Create app**.

### Step 2: Fill in Store Listing Details
Open [`PLAY_STORE_LISTING.md`](file:///C:/Users/Lithish%20r/.gemini/antigravity/scratch/BetShield/PLAY_STORE_LISTING.md) in this project and copy-paste the pre-written fields:
1. **Short description:** Paste from `PLAY_STORE_LISTING.md`.
2. **Full description:** Paste from `PLAY_STORE_LISTING.md`.
3. **Graphics:**
   * App Icon (512x512 PNG).
   * Feature Graphic (1024x500 PNG).
   * Phone Screenshots (Take 2-4 screenshots from your phone or the emulator preview).

### Step 3: Complete Policy Declarations & Privacy Policy
Open [`PLAY_STORE_DECLARATIONS.md`](file:///C:/Users/Lithish%20r/.gemini/antigravity/scratch/BetShield/PLAY_STORE_DECLARATIONS.md):
1. **Privacy Policy:** Upload the provided [`privacy_policy.html`](file:///C:/Users/Lithish%20r/.gemini/antigravity/scratch/BetShield/privacy_policy.html) to GitHub Pages or Google Sites, and paste the URL into Play Console.
2. **VpnService:** Paste the justification statement provided in `PLAY_STORE_DECLARATIONS.md`.
3. **AccessibilityService:** Paste the explanation statement provided in `PLAY_STORE_DECLARATIONS.md`.
4. **Data Safety:** Follow the checklist provided in `PLAY_STORE_DECLARATIONS.md`.

### Step 4: Upload the App Bundle (.aab)
1. In Play Console, go to **Release > Production** (or **Closed testing** for new accounts).
2. Click **Create new release**.
3. Drag and drop your signed `app-release.aab` generated in Part 1.
4. Enter Release notes: *"Initial release of BetShield with web filter, app guardian, custom blocker, and password lock."*
5. Click **Next** and **Save**.

### Step 5: Submit for Review
Click **Review and rollout** to send your app to Google's review team. Once approved (usually within 1–3 business days), your app will be live on Google Play worldwide!
