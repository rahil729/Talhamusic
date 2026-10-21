# Talha Rahil

Talha Rahil is a customized Android music player based on
[ViTune](https://github.com/bartoostveen/ViTune). It keeps the upstream app's
music features and adds Talha Rahil branding, a dark green-accented interface,
rounded media cards, and left-side navigation.

## Features

- Music playback from YouTube Music and local device files
- Background playback and offline caching
- Search, discovery, playlists, and lyrics
- Appearance customization and audio normalization
- Android Auto support and music-link handling

## Download

Download `Talha-Rahil.apk` from the
[latest Talha Rahil release](https://github.com/rahil729/Talhamusic/releases/latest).
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

Talha Rahil is derived from [ViTune](https://github.com/bartoostveen/ViTune).
The ViTune-derived code is licensed under the GNU General Public License,
version 3 (GPL-3.0); see [LICENSE](./LICENSE). Retain applicable copyright and
attribution notices and comply with GPL-3.0 when distributing the app or modified
versions, including applicable source-code requirements.

The Talha Rahil name, original branding, and project-specific modifications do
not transfer ownership of or change the license for ViTune-derived code.

This project is not affiliated with or endorsed by YouTube or Google. Their
trademarks and other intellectual property belong to their respective owners.
