# Talha Music

Talha Music is an Android music client built with Kotlin and Jetpack Compose. It combines YouTube search and playback through a FastAPI/yt-dlp resolver with Media3 playback, playlists, favorites, lyrics, and offline downloads.

## Features

- Search songs, artists, albums, and playlists
- Discover music by category
- Favorites, playlists, history, and offline downloads
- Queue controls, shuffle, repeat, playback speed, and sleep timer
- Lyrics lookup
- MediaSession background playback
- Compose UI with light and dark system themes

## Requirements

- Android Studio or the Android SDK
- JDK 17 or newer
- Android SDK platform 36
- Python 3.11+ for the YouTube search and stream backend

## Build the Android app

```powershell
.\gradlew.bat :app:assembleDebug
```

Install on a connected device or emulator:

```powershell
.\gradlew.bat :app:installDebug
```

## Download from GitHub

Every push builds a `TalhaMusic-debug` artifact in GitHub Actions for testing. It is signed with the debug key and is not the public release APK.

For public downloads, create a private signing keystore and add these repository Actions secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Encode the keystore file as Base64; never commit the keystore or its passwords. Push a version tag such as `v1.3.0` to build a signed APK and attach it to a GitHub Release. Share the release page link; users can download the APK directly from GitHub and install it on Android. Increment `versionCode` and update `versionName` in `app/build.gradle.kts` before each new release so Android accepts upgrades.

## YouTube search and playback backend

Online YouTube Music search and stream resolution use the hosted FastAPI backend at `https://talhamusic.onrender.com`. Public builds use this URL by default. The service must be deployed and kept running; an always-on Render instance requires a paid plan. If you deploy your own backend, pass its HTTPS URL with `-PstreamBackendUrl=...`.

For local development, start the backend:

```powershell
.\.venv\Scripts\Activate.ps1
python -m pip install -r backend\requirements.txt
python backend\server.py
```

Local Android emulator builds can use `http://10.0.2.2:8080`; for a physical phone, use your computer's LAN address:

```powershell
.\gradlew.bat :app:installDebug -PstreamBackendUrl=http://YOUR_COMPUTER_LAN_IP:8080
```

The phone and computer must be on the same network, and the backend listens on `0.0.0.0:8080`.

Search and first-time streaming require internet access. To listen offline, download tracks while online; saved tracks are available in Library > Downloads without Wi-Fi.

Public builds default to the HTTPS Render service. To build against another deployed backend, pass its URL explicitly:

```powershell
.\gradlew.bat :app:bundleRelease -PstreamBackendUrl=https://music-api.example.com
```

## Project structure

- `app/src/main/java/com/talha/music/ui` - Compose UI
- `app/src/main/java/com/talha/music/data` - Room, repository, and network data
- `app/src/main/java/com/talha/music/playback` - Media3 playback, queue, cache, and downloads
- `backend` - Optional FastAPI stream resolver

## Privacy and service notes

This project is an independent client. YouTube and LRCLIB are external services with their own availability and terms. Do not commit credentials, local properties, generated build folders, or private service configuration.

## License

Released under the MIT License. See [LICENSE](LICENSE).
