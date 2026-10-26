# Real AdMob Monetization Guide for BetShield

Follow this simple guide to start earning real passive income from BetShield while keeping the app completely free for your users.

---

## 1. How AdMob Earnings Work
* **100% Free for Users:** Users never pay a subscription or download fee.
* **Advertisers Pay Google:** Companies pay Google to show ads in your app.
* **Google Pays You:** Google shares 68% of the revenue with you and deposits it directly into your bank account every month (minimum payout threshold is $100).
* **Expected Revenue:**
  * **Adaptive Banner Ads:** Generates steady impressions as users browse the dashboard and custom blocker.
  * **Interstitial Ads:** Full-screen ads shown during transitions (~$4 to $10 per 1,000 views).
  * **Rewarded Video Ads:** Users voluntarily watch 15–30 second ads for emergency unlocks (~$15 to $35+ per 1,000 views).

---

## 2. Get Your Real AdMob IDs in 3 Minutes

1. **Sign Up for Google AdMob:**
   * Go to **[https://admob.google.com](https://admob.google.com)** and sign in with your Google account.
   * Complete payment profile (your name, address, and bank account for payouts).

2. **Add Your App:**
   * In the AdMob sidebar, click **Apps > Add App**.
   * Platform: **Android**.
   * Is the app listed on an app store? Choose **No** (until it's live on Google Play, then you can link it).
   * App name: **BetShield**.
   * Click **Add App**.
   * AdMob will give you your **App ID** (format: `ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`).

3. **Create Your 3 Real Ad Units:**
   * In AdMob, go to **Ad units > Add ad unit**:
     1. **Banner Ad:**
        * Select **Banner**, name it `BetShield Main Banner`, click **Create ad unit**.
        * Copy the Ad unit ID (format: `ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`).
     2. **Interstitial Ad:**
        * Select **Interstitial**, name it `BetShield Interstitial`, click **Create ad unit**.
        * Copy the Ad unit ID.
     3. **Rewarded Video Ad:**
        * Select **Rewarded**, name it `BetShield Emergency Unlock`, click **Create ad unit**.
        * Set reward amount to `1` and reward type to `Unlock`.
        * Copy the Ad unit ID.

---

## 3. How to Activate Real Ads in BetShield

You have two easy ways to activate real ads:

### Method A: Directly Inside the App (No Coding Required!)
1. Open BetShield on your phone.
2. In the top bar or under Quick Actions, tap the **Coin / Monetization Icon** (or **"AdMob Monetization & Real Ad IDs"**).
3. Switch **Real Live Ads Mode** to **ON**.
4. Paste your real **Banner ID**, **Interstitial ID**, and **Rewarded ID**.
5. Tap **Save & Activate**.
Real paying ads will immediately start serving!

### Method B: In Code (`AdConfig.kt`)
Open [`app/src/main/java/com/betshield/app/monetization/AdConfig.kt`](file:///C:/Users/Lithish%20r/.gemini/antigravity/scratch/BetShield/app/src/main/java/com/betshield/app/monetization/AdConfig.kt) and replace the sample IDs with your real IDs.
Also update your real App ID in [`app/src/main/AndroidManifest.xml`](file:///C:/Users/Lithish%20r/.gemini/antigravity/scratch/BetShield/app/src/main/AndroidManifest.xml) line 18:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="YOUR_REAL_ADMOB_APP_ID_HERE"/>
```
