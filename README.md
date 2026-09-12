# Talha Music

Talha Music is an Android music client built with Kotlin and Jetpack Compose. It combines local library management, YouTube Music discovery, Media3 playback, playlists, favorites, lyrics, downloads, and a small optional FastAPI stream resolver.

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
- Android SDK platform 34
- Python 3.11+ for the optional backend

## Build the Android app

```powershell
.\gradlew.bat :app:assembleDebug
```

Install on a connected device or emulator:

```powershell
.\gradlew.bat :app:installDebug
```

## Download from GitHub

Every push builds the app with GitHub Actions. Open the repository's **Actions** tab, select **Android APK**, open the latest successful run, and download the `TalhaMusic-debug` artifact. This APK is signed with the Android debug key for direct sideloading.

For a public release, create a GitHub Release and sign the APK with a private release keystore. Never commit the keystore or its passwords.

## Optional stream backend

The app works with NewPipe/Piped fallbacks. The optional local resolver uses FastAPI and yt-dlp:

```powershell
.\.venv\Scripts\Activate.ps1
python -m pip install -r backend\requirements.txt
python backend\server.py
```

The debug build uses the Android emulator host address `http://10.0.2.2:8080` by default. For a physical phone, provide your computer's LAN address when building:

```powershell
.\gradlew.bat :app:installDebug -PstreamBackendUrl=http://YOUR_COMPUTER_LAN_IP:8080
```

The phone and computer must be on the same network, and the backend must listen on `0.0.0.0:8080`.

For a release build, deploy the backend behind HTTPS and pass its public URL explicitly. If no URL is supplied, the app skips the optional resolver and uses its built-in extraction fallbacks:

```powershell
.\gradlew.bat :app:assembleRelease -PstreamBackendUrl=https://music-api.example.com
```

## Project structure

- `app/src/main/java/com/talha/music/ui` - Compose UI
- `app/src/main/java/com/talha/music/data` - Room, repository, and network data
- `app/src/main/java/com/talha/music/playback` - Media3 playback, queue, cache, and downloads
- `backend` - Optional FastAPI stream resolver

## Privacy and service notes

This project is an independent client. YouTube, YouTube Music, Piped, NewPipe, and LRCLIB are external services with their own availability and terms. Do not commit credentials, local properties, generated build folders, or private service configuration.

## License

Released under the MIT License. See [LICENSE](LICENSE).
