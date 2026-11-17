# RED4U

RED4U is an Android live-TV player for Android phones and Android TV. It loads M3U/M3U8 playlists and plays supported streams with AndroidX Media3 (ExoPlayer).

## Current MVP
- Arabic and English interface strings (follows the device language).
- Built-in multi-source catalog loaded automatically on first launch.
- Built-in catalog combines Algeria, Arabic-language, and international playlists.
- Separate **Built-in** button returns to the default catalog at any time.
- Manual M3U/M3U8 URL entry remains available; the last custom URL is remembered.
- GitHub `blob` file links are converted to `raw.githubusercontent.com` automatically.
- Parses channel names, `group-title`, and `tvg-logo` metadata.
- HLS playback support through Media3.
- Android phone and Android TV launcher entry points.

## Built-in channel sources
The default catalog merges these public M3U sources and removes duplicate stream URLs:

- Algeria: `https://iptv-org.github.io/iptv/countries/dz.m3u`
- Arabic-language channels: `https://iptv-org.github.io/iptv/languages/ara.m3u`
- International free broadcaster streams: `https://broadcastatlas.com/official.m3u`

These are third-party channel indexes. Public availability does not itself prove that a stream is licensed for redistribution or accessible in every country. RED4U does not host or relay video streams and is not affiliated with the listed broadcasters or playlist providers. Channel availability, geographic restrictions, and playback compatibility can change.

The built-in catalog is fetched over the internet; it is not an offline bundle. Users can instead enter a personal or otherwise authorized M3U/M3U8 URL in the manual field.

## Manual playlist URL
A direct raw GitHub URL is recommended, for example:

```
https://raw.githubusercontent.com/OWNER/REPOSITORY/BRANCH/path/channels.m3u
```

A normal GitHub file URL containing `/blob/` is also accepted and converted by the app.

## Build
Open the project in Android Studio (JDK 17, Android Gradle Plugin 8.7.3) or run:

```bash
gradle assembleDebug
```

The APK output is:

```
app/build/outputs/apk/debug/app-debug.apk
```

Pushing to `main`, or manually starting the **Build RED4U APK** workflow, builds and uploads the debug APK as a GitHub Actions artifact.

## Planned improvements
Favorites, multi-source management, in-app language switching, EPG, channel search, and polished Android TV remote navigation are planned for later iterations.
