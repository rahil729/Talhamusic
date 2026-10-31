# Talha Music

Talha Music is an independent Android music player created, designed, and owned by Talha.

## Features

- Music playback from YouTube Music and local device files
- Background playback and offline caching
- Search, discovery, playlists, and lyrics
- Appearance customization and audio normalization
- Android Auto support and music-link handling

## Download

Download `Talha-Rahil.apk` from the
[latest Talha Music release](https://github.com/rahil729/Talhamusic/releases/latest).
On Android, open the downloaded APK and follow the installation prompts. Android
may ask you to allow installation from your browser or file manager.

## Build

On Windows, run from this directory:

```powershell
.\gradlew.bat :app:assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
The project requires JDK 25, Android SDK 37, and NDK 29.0.14206865.

To publish a release, push a tag matching `v*`. GitHub Actions builds the
APK and attaches it to a public GitHub release as `Talha-Rahil.apk`.

## Copyright and license

This project is licensed under the MIT License. See [LICENSE](./LICENSE) for details.

Copyright © 2026 Talha. All rights reserved.

This project is not affiliated with or endorsed by YouTube or Google. Their
trademarks and other intellectual property belong to their respective owners.
