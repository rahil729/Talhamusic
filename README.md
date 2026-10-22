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

Download the latest
[Talha-Rahil.apk](https://github.com/rahil729/Talhamusic/releases/latest/download/Talha-Rahil.apk)
directly, or browse [all Talha Rahil releases](https://github.com/rahil729/Talhamusic/releases/latest).
On Android, open the APK and follow the installation prompts. Android may ask
you to allow installation from your browser or file manager.

## Project website

The project website is published with GitHub Pages at
https://rahil729.github.io/Talhamusic/. It describes the app and provides a
direct APK download. The repository root contains the website source,
`robots.txt`, and `sitemap.xml`.

## Find the website on Google

After the site is published, add its URL as a URL-prefix property in
[Google Search Console](https://search.google.com/search-console/), verify
ownership, submit `sitemap.xml`, and request indexing for the homepage. Google
decides whether and when to index the site and where it ranks; submission does
not guarantee a particular search position.

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
