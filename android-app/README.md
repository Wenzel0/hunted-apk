# Hunted Threshold: Android app (APK)

A small, fast viewer that opens the game **in landscape and fullscreen** on Android.

What it does
- Landscape (either way up), immersive fullscreen (system bars hidden, swipe to peek), keeps the screen on, uses the notch area.
- Runs the game **fully offline from inside the app** when the game is bundled (see below), or opens a server address you type (for example `http://192.168.1.5:3000/` where `node server.js` is running, so you get the shared accounts).
- Sounds start without extra taps, sounds stop when you leave the app, fullscreen video works.
- Back button = menu: Resume / Reload / Change address / Exit. If the server is unreachable you get a "Try again / Change address" page.
- Accounts and settings are saved inside the app (localStorage). Bundled mode uses local accounts; use a server address for the shared database.

## Build the APK (pick one)

**A. GitHub (no installs):** put this project in a GitHub repo, open **Actions > Build APK > Run workflow**, then download the `HuntedThreshold-apk` artifact (`app-debug.apk`).

**B. Android Studio:** open the `android-app` folder, run `./copy_game.sh` (or `copy_game.bat` on Windows) to bundle the game, then **Build > Build APK(s)**. The file appears in `app/build/outputs/apk/debug/`.

**C. Command line** (JDK 17 + Android SDK + Gradle 8.7+): `cd android-app && ./copy_game.sh && gradle assembleDebug`

No bundled game? Skip `copy_game`: the APK is then a pure viewer that asks for the game address on first start (about 3 MB).

## Install on the phone
Copy `app-debug.apk` to the phone, open it and allow "install unknown apps" when asked. The app is debug-signed. For a store release create a keystore and a signed release build (`gradle assembleRelease` with your signing config).

## No APK needed: install from Chrome
The game is also a web app: open it over https (or localhost) in Chrome, then menu > **Install app** (or Settings > APP > Install). It opens fullscreen in landscape from the home screen.
