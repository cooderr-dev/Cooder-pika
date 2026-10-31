# WLLPR for Android

Native Android (Kotlin + Jetpack Compose) version of the WLLPR wallpaper app.
Wallpapers are generated on-device from code: 6 styles x 14 palettes x dark/light (168 ready-made),
plus a Create screen with live preview, shuffle, and your own combinations.

## Features
- Home: featured wallpaper + a carousel per style, dark/light app theme (remembered)
- Detail: full-screen preview, **Set wallpaper** (home / lock / both) and **Save** to Pictures/WLLPR
- Create: pick style, palette, dark/light, shuffle the seed, lock-screen clock preview
- Wallpapers render at your phone's real screen resolution (max 1440 px wide)
- Requires Android 10 (API 29) or newer. No internet, no ads, no extra permissions.

## Build it (pick one)

### A) Android Studio
1. Install Android Studio (Ladybug or newer).
2. File > Open > select this folder, wait for Gradle sync.
3. Plug in a phone (USB debugging on) or start an emulator, press Run.
4. APK file: Build > Build APK(s) -> `app/build/outputs/apk/debug/app-debug.apk`

### B) No install: GitHub Actions
1. Create a GitHub repo and push this folder to the `main` branch.
2. Open the Actions tab > "Build APK" > wait ~3 minutes.
3. Download the `wllpr-debug-apk` artifact, unzip, and install the APK on your phone.

## Customise
- App id / name: `applicationId` and `namespace` in `app/build.gradle.kts`, `strings.xml`
- New palettes: add a `Palette(...)` in `Engine.kt`
- New styles: add a function in `Engine.kt` and a case in `Painter.draw`
- Home sections / seeds: `Catalog.kt`
- Play Store: replace the debug signing config in `app/build.gradle.kts` with your own keystore
