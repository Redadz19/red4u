# RED4U

RED4U is an Android live-TV player that loads user-provided M3U playlists and plays streams with AndroidX Media3 (ExoPlayer).

## Current MVP
- Arabic and English strings (follows the device language).
- Load an M3U playlist from a URL.
- Convert GitHub `blob` file links to `raw.githubusercontent.com` links automatically.
- Parse channel names and `group-title` / `tvg-logo` metadata.
- Play selected HTTP(S) streams with the built-in player.
- Remember the last playlist URL locally.
- Android phone and Android TV launcher entry points.
- GitHub Actions debug APK build.

## Playlist URL
Use a direct raw GitHub URL, for example:

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

## Notes
- RED4U does not bundle TV channels. Users must provide playlists and have the right to access their streams.
- This is the first implementation stage. Favorites, multi-source management, in-app language switching, EPG, and polished Android TV navigation are planned for later iterations.
