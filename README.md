<p align="center">
  <img src="store/icon-512.png" width="112" alt="Mantra Mala app icon">
</p>

<h1 align="center">ॐ Mantra Mala — 108 Japa Counter for Android</h1>

<p align="center">
A standalone, fully offline japa (mantra chanting) counter with a modern Hindu design.<br>
No server, no account, no ads, no analytics. The app doesn't even request the internet permission.
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.sjpixels.mantramala">
    <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80">
  </a>
</p>

<p align="center">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Target API 36" src="https://img.shields.io/badge/target%20API-36-3DDC84">
  <img alt="Kotlin 2.3" src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="License GPL-3.0" src="https://img.shields.io/badge/license-GPL--3.0-blue">
  <br>
  <img alt="100% offline" src="https://img.shields.io/badge/100%25-offline-d9560b">
  <img alt="No ads" src="https://img.shields.io/badge/ads-none-d9560b">
  <img alt="No tracking" src="https://img.shields.io/badge/tracking-none-d9560b">
  <img alt="9 languages" src="https://img.shields.io/badge/languages-9-d9560b">
  <img alt="18 unit tests" src="https://img.shields.io/badge/unit%20tests-18%20passing-success">
</p>

<p align="center">
<img src="store/screenshots/screenshot-01-japa-light.png?v=1.1.1" width="160" alt="Counter, light mode" title="Counter, light mode">
<img src="store/screenshots/screenshot-02-voice-light.png?v=1.1.1" width="160" alt="Voice japa samples" title="Voice japa samples">
<img src="store/screenshots/screenshot-03-japa-dark.png?v=1.1.1" width="160" alt="Counter, night mode with Yantra" title="Counter, night mode with Yantra">
<img src="store/screenshots/screenshot-04-mantras-light.png?v=1.1.1" width="160" alt="Mantra library" title="Mantra library">
<img src="store/screenshots/screenshot-05-history-light.png?v=1.1.1" width="160" alt="Lifetime history" title="Lifetime history">
<img src="store/screenshots/screenshot-06-themes-dark.png?v=1.1.1" width="160" alt="Themes and settings" title="Themes and settings">
<img src="store/screenshots/screenshot-07-hindi-lotus.png?v=1.1.1" width="160" alt="Hindi, lotus style" title="Hindi, lotus style">
<img src="store/screenshots/screenshot-08-overlay-home.png?v=1.1.1" width="160" alt="Floating bead over other apps" title="Floating bead over other apps">
</p>

## ✨ Features

- 📿 **Counter**: tap anywhere on the mala (or use the + button) to count. Use − to correct a miscount and *Reset* to clear the current round (with confirmation). At **108** the round completes: you get a strong vibration and temple bell, a 🙏 banner, the mala is saved to history, and the counter starts again at 0.
- 🕉️ **Default mantra: Om Namah Shivaya** (ॐ नमः शिवाय). Change it from the 14-mantra library or add your own.
- 📊 **Analytics on the home screen**: today vs. daily goal, streak, total malas, total japa.
- 📜 **Lifetime history**: every mala is kept for life from the day the app is installed ("Lifetime record since …"). It shows totals, current and best streak, active days, average time per mala and a last-7-days chart, and lists everything by **month** (latest open, older months folded) and day. Delete one entry (with **Undo**) or a whole day, or use **Clear past history** to remove everything before today, anything older than 30 days or 1 year, or all of it. Each choice shows how many malas it will delete first.
- 🎨 **Themes**: 9 sacred palettes (Saffron, Kumkum, Gold, Krishna Blue, Tulsi, Lotus, Rudraksha, Peacock, Ash), 3 counter styles (Mala beads, Padma/Lotus, Yantra), and a **day/night toggle** in the top bar.
- 🌐 **9 languages** from the **flag dropdown** in the top bar: Hindi, English, Russian, Spanish, German, French, Chinese, Japanese, Korean. The choice also appears in Android 13+ *Settings → Apps → Language*.
- 🔴 **Floating counter bead**: a movable bead shown on every screen. Tap to count, drag to move, and the position is remembered. Its colour follows the theme or can be set from 12 swatches or any hue, and its **size** is adjustable from 48 to 112 dp. It can be hidden in Settings.
  - **Keep over other apps** (optional): the bead stays on screen after you leave or close the app, working over the home screen and other apps. It needs Android's *Display over other apps* permission (the app guides you to it) and runs as a foreground service with an ongoing notification that has a **Turn off** action. Long-press the bead to return to the app.
- 🔔 **Practice options**: vibration, bell, count with the volume keys, and keep the screen on.
- 💾 **Everything persists**: the in-progress count is saved on every bead, and settings and history survive restarts. Android's own backup or phone transfer can carry them to a new phone.
- 🔗 **"Built by sjpixels.com"**: a quiet, tappable signature under the tab bar and in Settings → About. It opens http://sjpixels.com/ in the browser.
- 📱 Works in portrait and landscape, phones and tablets, and with TalkBack.

## 🛠️ Tech

| | |
|---|---|
| Language / UI | Kotlin 2.3, Jetpack Compose (BOM 2026.06.01), Material 3 |
| Storage | Room 2.8 (history, custom mantras), DataStore (settings, current count) |
| Languages | AppCompat per-app locales, `generateLocaleConfig` |
| Build | AGP 8.13.2, Gradle 8.14.3, JDK 17, **compileSdk/targetSdk 36**, minSdk 26 |
| Art | 100% vector: counter dials drawn with Canvas; icon generated from the Tiro Devanagari ॐ glyph |
| Fonts | Cinzel and Tiro Devanagari Sanskrit (SIL OFL, see `licenses/`) |

```
app/src/main/java/com/sjpixels/mantramala/
├── MainActivity.kt          splash, edge-to-edge, volume-key counting
├── MantraMalaApp.kt         app + tiny dependency container
├── data/                    Room DB, DataStore settings, mantra catalogue, CounterRepository (the one shared counter), pure counting/stats logic
├── ui/MainViewModel.kt      single source of truth for the UI
├── ui/AppRoot.kt            scaffold, tabs, undo snackbar, keep-screen-on
├── ui/counter/              CounterScreen + DialArt (Mala / Padma / Yantra)
├── ui/mantras/              library + add-your-own dialog
├── ui/history/              stats, weekly chart, day-grouped records
├── ui/settings/             themes, styles, language, practice options
├── ui/components/           top/bottom bars, language flags, ornaments, in-app FloatingBead
├── overlay/                 FloatingCounterService (bead over other apps), BeadView, FloatingOverlay
└── ui/theme/                9 palettes × day/night, typography, sharp cut-corner shapes
```

## ▶️ Build and run

Requires JDK 17 and the Android SDK. Both are installed on this Mac: JDK at `/opt/homebrew/opt/openjdk@17`, SDK at `~/Library/Android/sdk`. The easiest route is **Android Studio → Open** this folder and press ▶. From the terminal:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew testDebugUnitTest      # 18 unit tests: counting, 108 roll-over, streaks, stats, default mantra, clear-past ranges
./gradlew installDebug           # install on a connected phone/emulator (app id: com.sjpixels.mantramala.debug)
./gradlew bundleRelease          # Play Store bundle → app/build/outputs/bundle/release/app-release.aab
```

An emulator is set up as well: `~/Library/Android/sdk/emulator/emulator -avd MantraMala_API36`.

## 🚀 Publishing

**Live on Google Play:** <https://play.google.com/store/apps/details?id=com.sjpixels.mantramala>

Follow **[docs/PLAY_STORE_LAUNCH.md](docs/PLAY_STORE_LAUNCH.md)**. All store texts are in [store/LISTING.md](store/LISTING.md), graphics are in `store/`, and the privacy policy is in [docs/privacy-policy.html](docs/privacy-policy.html).
Optional ChatGPT prompts for extra artwork: [docs/CHATGPT_PROMPTS.md](docs/CHATGPT_PROMPTS.md).

## 🧩 Customising

- **Default mantra**: `Mantras.DEFAULT_KEY` in `data/Mantras.kt`.
- **Add a built-in mantra**: add an entry to `Mantras.builtIn`, plus `m_<id>_title` / `m_<id>_meaning` in every `res/values*/strings.xml`.
- **Add a language**: create `res/values-<code>/strings.xml` (copy the English file), then add a flag entry to `AppLanguages.all` in `ui/components/Language.kt`.
- **Colours**: `ui/theme/Palettes.kt`.
- **Icon and store graphics**: edit `tools/assets.py`, then run `python tools/assets.py .` (needs `pip install -r tools/requirements.txt` and `brew install librsvg`).

## 📄 License

Code: GPL-3.0 (see `LICENSE`). Fonts: SIL Open Font License 1.1 (see `licenses/`).
