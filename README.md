# Android UI Inspector

An on-device UI inspector for Android 11+ (API 30+), built with Kotlin, Compose and Material 3.

## Build and installation

Use JDK 17+ and Android SDK 37. Open this directory in Android Studio, or run:

```sh
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows use `gradlew.bat`. ADB is only a development tool; the installed app runs independently.

## Setup

1. Open UI Inspector.
2. Open Accessibility Settings and manually enable UI Inspector.
3. Return to the app and tap Start Inspector.
4. Switch to the target app.

The service reads accessibility properties and requests screenshots only for local color analysis. No network permission or upload is used.

## Limitations

Accessibility exposes target-app semantics, not a complete View or Compose layout hierarchy. Secure windows prevent screenshots. Canvas, games and WebViews may expose limited nodes.
UI Insp on android
