# Google Play Console Policy Declarations Guide for QuitBet

Google Play requires specific policy declarations for apps using `VpnService`, `AccessibilityService`, and Google AdMob. Use these exact answers to pass review without rejection.

---

## 1. VpnService Declaration
In Google Play Console, go to **Policy > App content > VpnService policy**:

1. **Does your app use the VpnService API?**
   * Select: **Yes**

2. **Select the primary category for your app's use of VpnService:**
   * Select: **"Personal safety / Content filtering / Parental control"** or **"Device security"**

3. **Justification Statement (paste this exact text):**
   ```text
   QuitBet uses VpnService to create a local loopback DNS sinkhole strictly on the user's device (10.1.10.1) to block connections to known online gambling, betting, and malicious casino domains as well as user-specified custom domains. No user traffic is routed to external proxy servers or collected. This core functionality is essential to provide self-exclusion and habit protection against gambling addiction.
   ```

---

## 2. AccessibilityService API Declaration
In Google Play Console, go to **Policy > App content > Accessibility services**:

1. **Why does your app need the AccessibilityService API?**
   * Select: **"Digital wellbeing / Parental control / App blocking"**

2. **Provide detailed explanation of how your app uses the Accessibility API (paste this exact text):**
   ```text
   QuitBet uses the AccessibilityService API to detect the package name of foreground applications and active browser address bar URLs in real time. If the foreground package or URL matches a known gambling platform (e.g., online casinos, sportsbooks) or a user-selected blocked application (e.g., social media or games during focus periods), QuitBet performs the GLOBAL_ACTION_HOME action to return the user to the home screen and displays a blocking intervention overlay. It also prevents unauthorized bypass or uninstallation via Android Settings without entering the user's configured master password. QuitBet does NOT inspect, read, or collect user keystrokes, personal messages, or credentials.
   ```

3. **YouTube Video Link (Google Requirement):**
   * Google requires a 30-to-60-second video demonstrating:
     1. The prominent in-app disclosure screen (which is built into QuitBet's `SetupPermissionsActivity`).
     2. Navigating to Android Accessibility settings to enable QuitBet.
     3. Opening a betting or blocked app and showing QuitBet blocking it.
   * Upload this video as **Unlisted** on YouTube and paste the link into the Play Console form.

---

## 3. Data Safety Form
In Google Play Console, go to **Policy > App content > Data safety**:

* **Does your app collect or share user data?**
  * Select: **Yes** (because of the Google Mobile Ads / AdMob SDK).
* **Data collected by Google Mobile Ads (AdMob):**
  * **Device or other IDs:** Device ID / Advertising ID (used for Analytics & Advertising / Marketing).
  * **Location:** Coarse location (collected by AdMob for ad targeting, not by BetShield directly).
  * **App info and performance:** Crash logs, diagnostics (used for app analytics).
* **Are all data transfers encrypted in transit?**
  * Select: **Yes** (all ad communication is over HTTPS).
* **Do you provide a way for users to request data deletion?**
  * Select: **Yes** (state that no personal user data is stored on external servers; uninstalling the app deletes all local configurations).

---

## 4. Ads Declaration
* **Does your app contain ads?**
  * Select: **Yes, my app contains ads** (Google Mobile Ads).
