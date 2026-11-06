# Launching Mantra Mala on Google Play — step by step

This guide takes you from this repository to a live Play Store listing. Requirements were checked in **November 2026**. Google changes them every year, so if a screen in Play Console looks different, trust Play Console.

**Already done for you in this repo**

| Item | Where |
|---|---|
| App targets **Android 16 (API 36)**. Play has required this for new apps since 31 Aug 2026 | `app/build.gradle.kts` |
| Release build with R8 shrinking, tested on an Android 16 emulator | `./gradlew bundleRelease` |
| 512 px icon, 1024×500 feature graphic, 8 phone screenshots | `store/` |
| Title, short and full description (9 languages) | `store/LISTING.md` |
| Privacy policy page | `docs/privacy-policy.html` |
| No internet permission, no ads, no analytics. Permissions: `VIBRATE`, the optional floating-bead ones (`SYSTEM_ALERT_WINDOW`, foreground service "special use", notifications), and the optional voice-sample ones (`RECORD_AUDIO`, foreground service "media playback", `WAKE_LOCK`) | `app/src/main/AndroidManifest.xml` |
| All 9 languages delivered to every phone (Play's language splitting is turned off so the flag menu always works) | `app/build.gradle.kts` → `bundle { language { enableSplit = false } }` |

**Time and cost:** $25 one-time registration fee. Expect **3–4 weeks** in total if you open a *personal* account: identity check, then a mandatory 14-day closed test, then production review.

---

## Step 1 — Create your Google Play developer account (day 1)

1. Go to <https://play.google.com/console/signup> and sign in with the Google account you want to own the app. Use one you'll keep for years.
2. Choose the account type:
   - **Personal**: fastest. You must run a closed test with **at least 12 testers opted in for 14 continuous days** before you can publish to production (step 9). This applies to personal accounts created after 13 Nov 2023.
   - **Organization**: needs a registered business and a free **D-U-N-S number** (from Dun & Bradstreet, which can take days or weeks). It skips the 12-tester rule.
3. Pay the **$25** fee.
4. Complete **identity verification** (government ID, address, phone). Use your legal name exactly as it appears on your ID. This also covers Android's 2026 developer-verification programme for apps distributed through Play. If Play Console later asks you to confirm your app's package name, do it there.
5. Verify the contact email and phone number Play Console asks for.

## Step 2 — Decide your final package name (do this before the first upload)

The app ID is `com.sjpixels.mantramala`. **It can never change after the first upload.** To use a different one, edit `applicationId` in `app/build.gradle.kts` now (for example `com.yourname.mantramala`). The `namespace` line can stay as it is.

## Step 3 — Create your upload key (one time, keep it safe forever)

On your Mac, in the project folder:

```bash
# Uses the JDK 17 installed for this project
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
$JAVA_HOME/bin/keytool -genkeypair -v \
  -keystore upload-keystore.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Answer the questions (name, organisation, country) and choose a strong password. Then:

```bash
cp keystore.properties.example keystore.properties
# edit keystore.properties: put the passwords you just chose
```

**Back up `upload-keystore.jks` and both passwords** to a password manager *and* an offline copy. Both files are git-ignored. If you lose the upload key, Google can reset it (Play App Signing keeps the real app-signing key), but that takes days.

## Step 4 — Build the release bundle

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew clean bundleRelease
```

Output: **`app/build/outputs/bundle/release/app-release.aab`** (about 5 MB). This signed bundle is the file you upload.

> Opening the project in **Android Studio** also works: *File → Open* this folder, then *Build → Generate Signed App Bundle*. The SDK is already installed at `~/Library/Android/sdk`, which Android Studio uses automatically.

**For every future update**, raise `versionCode` (1 → 2 → 3…) and `versionName` ("1.0.1") in `app/build.gradle.kts` before building.

## Step 5 — Publish the privacy policy (Play requires a URL even for apps that collect nothing)

1. Open `docs/privacy-policy.html` and replace **`[YOUR NAME OR STUDIO NAME]`** and **`[YOUR SUPPORT EMAIL]`**.
2. Host it. The simplest free option is GitHub Pages:
   - Push this repo to GitHub (it can be public, or use a separate small public repo that holds only this file).
   - Go to *Repo → Settings → Pages → Build and deployment*, choose *Deploy from a branch*, branch `main`, folder **`/docs`**, then Save.
   - After about a minute your URL is `https://<your-username>.github.io/<repo-name>/privacy-policy.html`.
3. Open the URL in a browser to confirm it loads. You'll paste it into Play Console twice (steps 7a and 7g).

## Step 6 — Create the app in Play Console

*Play Console → Home → Create app*

- App name: `Mantra Mala: 108 Japa Counter`
- Default language: English (United States), or Hindi if India is your main audience
- App or game: **App**. Free or paid: **Free** (an app that has been published as free can never later become paid)
- Tick the declarations (Developer Program Policies, US export laws), then **Create app**.

## Step 7 — Complete "App content" (*Policy and programs → App content*)

Answer each section like this:

| Section | Answer for this app |
|---|---|
| **a. Privacy policy** | Paste your URL from step 5 |
| **b. App access** | *All functionality is available without special access* |
| **c. Ads** | **No**, my app does not contain ads |
| **d. Content rating** | Start the questionnaire. Category: *All other app types* (or *Reference, news or educational*). Answer **No** to violence, sexuality, language, drugs, gambling, user interaction/sharing, location sharing and purchases. Expect a rating of *Everyone* / *PEGI 3* / *IARC 3+*. |
| **e. Target audience and content** | Choose **13–15, 16–17 and 18+**. Leave out the under-13 groups unless you want to opt into the stricter *Families* programme. Answer "Could your store listing unintentionally appeal to children?" with *No*. |
| **f. News app** | No |
| **g. Data safety** | "Does your app collect or share any of the required user data types?" → **No**. Everything stays on the device and the app has no internet permission. Voice recordings also count as not collected: Google's definition of "collected" is data sent off the device, and recordings are only processed and stored on the phone. Then review and **Submit**. The privacy policy URL goes here as well. |
| **h. Advertising ID** | **No**, the app does not use an advertising ID |
| **i. Government apps** | No |
| **j. Financial features** | My app doesn't provide any financial features |
| **k. Health apps** | My app does not have any health features. It is a devotional counter, not a medical app. |
| **l. Foreground service permissions** | Appears because the app uses two foreground service types. Tick **Media playback** (voice samples keep playing and counting with the screen off) and **Special use** (the floating bead), and paste the descriptions below. Play also asks for a **short video**: record your phone screen (Quick Settings → *Screen record*) showing Settings → *Keep over other apps* → leaving the app → tapping the bead over the home screen → *Turn off* in the notification. Upload it to YouTube as **Unlisted** and paste the link. |

**Text for the foreground-service declaration (Media playback):**
```
Voice sample playback. The user can record or choose an audio file of a mantra and play it on repeat; the app counts each complete play (108 plays = one mala). The media playback foreground service keeps the audio playing and counting when the screen is off or the user switches apps, which is the core purpose of the feature. It starts only when the user presses Play, shows a notification with the count plus Pause and Stop, and ends as soon as playback is stopped.
```
For the video, show: Japa screen → *Add a voice sample* → record → Play → lock the phone (audio keeps playing, notification shows the count) → *Stop* in the notification.

**Text for the foreground-service declaration (Special use):**
```
Floating japa counter. When the user turns on "Keep over other apps" in Settings, a small movable counter bead stays on screen over other apps so they can keep counting mantra repetitions (japa) while using their phone. The foreground service keeps that bead alive after the user leaves the app. It runs only while the user has this option on, shows an ongoing notification with the current count and a "Turn off" button, and stops immediately when turned off. No data is collected or sent anywhere; the app has no internet permission.
```

> The *Display over other apps* permission (`SYSTEM_ALERT_WINDOW`) doesn't need its own declaration. Reviewers do check that it's used only for a user-visible, user-enabled feature, and here it's off until the user turns it on.

## Step 8 — Store listing (*Grow users → Store presence → Main store listing*)

Copy everything from **`store/LISTING.md`**:

1. App name, short description, full description.
2. **App icon**: `store/icon-512.png`
3. **Feature graphic**: `store/feature-graphic-1024x500.png`
4. **Phone screenshots**: all eight files in `store/screenshots/` (order 01 → 08).
5. Optionally upload the same screenshots to the 7-inch and 10-inch tablet slots so the app is shown to tablet users.
6. *Store settings*: Category **Lifestyle**, tags, contact email (required, public), and optionally a website.
7. *Manage translations*: add Hindi and the other languages from `store/LISTING.md`. Optional, but it noticeably helps in India and abroad.

## Step 9 — Testing tracks

### 9a. Internal testing (minutes; for you and up to 100 people)
*Test and release → Testing → Internal testing → Create new release*
1. On first use, accept **Play App Signing** (recommended; Google keeps the app-signing key safe).
2. Upload `app-release.aab`, add release notes (from LISTING.md), then *Save → Review release → Start rollout*.
3. Under *Testers*, create an email list with your own Gmail, open the **opt-in link** on your phone, and install from Play. Check: tap through a full mala, history, themes, the language flags, and the floating bead (including *Keep over other apps* and *Turn off* in the notification).

### 9b. Closed testing: 12 testers for 14 days (required for personal accounts)
*Test and release → Testing → Closed testing → Create track* (or use the default "Alpha")
1. Create a release with the same AAB (*Promote release* from internal testing works too).
2. Add testers. The easiest way is a **Google Group** (groups.google.com): create one, invite friends and family, and add the group's email as the tester list. You need **at least 12 people who actually opt in** through the link and install the app. Invited is not the same as opted in.
3. Choose countries (all countries is fine), then *Review → Start rollout*. The closed track also needs Google's review the first time, usually within a few days.
4. Keep **at least 12 testers opted in for 14 days in a row**. If someone leaves, the count can drop below 12, so recruit 15–20 to be safe. Ask testers to actually open the app a few times; Google asks how testing went.
5. Fix anything testers report. Upload new builds with a higher `versionCode`.

## Step 10 — Apply for production access (personal accounts)
After 14 days, Play Console *Dashboard* shows **Apply for production**. Answer the short questionnaire honestly (how you recruited testers, what feedback you got, what you changed). Review takes up to about **7 days**.

## Step 11 — Release to production
*Test and release → Production → Countries/regions*: add all countries, or start with India.
Then *Create new release* → choose the AAB from your library → release notes → *Review release*.
- A **staged rollout** (for example 20%, then 50%, then 100%) is a sensible safety net.
- *Send for review*. First reviews usually take a few hours to a few days. You'll get an email when the app is live.

## Step 12 — After launch
- **Updates**: raise `versionCode`/`versionName`, run `./gradlew bundleRelease`, then upload to Production, or to internal testing first.
- **Android vitals** (*Monitor and improve*) shows any crashes and ANRs. Line numbers are kept in release builds so crash reports stay readable.
- **Yearly target-API rule**: each August Google raises the minimum. Expect to move to **API 37 by about August 2027**: update `compileSdk`/`targetSdk` and the libraries (Android Studio's *AGP Upgrade Assistant* handles most of it).
- Reply to reviews. A short "Hari Om, thank you" goes a long way.

---

## Final checklist

- [ ] Developer account created, $25 paid, identity verified
- [ ] Package name final (`applicationId`)
- [ ] `upload-keystore.jks` created **and backed up**
- [ ] `keystore.properties` filled in, `./gradlew bundleRelease` succeeds
- [ ] Privacy policy edited (name, email) and hosted; URL opens in a browser
- [ ] App content: privacy, access, ads, rating, audience, data safety, ad ID, gov, financial, health, **foreground service (special use + video)**
- [ ] Store listing: text, icon, feature graphic, 8 screenshots, category, contact email
- [ ] Internal test installed and checked on a real phone
- [ ] Closed test: 12+ testers opted in for 14 consecutive days
- [ ] Production access approved, then production release sent for review

### References
- Target API requirement: <https://developer.android.com/google/play/requirements/target-sdk>
- Testing requirement for new personal accounts: <https://support.google.com/googleplay/android-developer/answer/14151465>
- Data safety form: <https://support.google.com/googleplay/android-developer/answer/10787469>
- Developer verification: <https://developer.android.com/developer-verification>
- Foreground service types and Play declaration: <https://developer.android.com/develop/background-work/services/fgs/service-types> · <https://support.google.com/googleplay/android-developer/answer/13392821>
