# RED4U

RED4U is an Android live-TV player for Android phones and Android TV. It loads M3U/M3U8 playlists and plays supported streams with AndroidX Media3 (ExoPlayer).

## Current MVP
- Arabic and English interface strings (follows the device language).
- Built-in default channel catalog, loaded automatically on first launch.
- Separate **Built-in** button to return to the default catalog at any time.
- Manual M3U/M3U8 URL entry remains available; the last custom URL is remembered.
- GitHub `blob` file links are converted to `raw.githubusercontent.com` automatically.
- Parses channel names, `group-title`, and `tvg-logo` metadata.
- HLS playback support through Media3.
- Android phone and Android TV launcher entry points.

## Built-in channel catalog
The default catalog currently uses this public M3U feed:

```
https://broadcastatlas.com/official.m3u
```

The feed describes streams that are offered free on broadcasters' own websites and is maintained by a third party. RED4U does not host or relay those streams and is not affiliated with the feed provider or broadcasters. A stream being publicly accessible does not by itself establish a redistribution licence. Availability, geographic restrictions, and playback compatibility can change.

The built-in catalog is fetched over the internet; it is not an offline bundle. The manual playlist field can be used instead with a personal or otherwise authorized M3U/M3U8 source.

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
