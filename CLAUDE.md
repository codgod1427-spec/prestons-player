# Preston's Player — Claude Code instructions

An Android TV / Fire TV IPTV player with a cable-provider-style grid guide
(dark navy theme, timeline EPG, channel column, full-screen ExoPlayer playback).
User loads their own M3U playlist + optional XMLTV EPG. The app ships no content.

## Splash screen
`ui/SplashScreen.kt` — animated parkour loading screen. An original blocky voxel
runner sprints, leaps a gap, hops up a ledge, and drops back down on a loop.
Everything is drawn from primitives in a Compose `Canvas` (no image assets, no
third-party game art). Tune the course by editing the `COURSE` list of `Move`s
and the `PLATFORMS` list — world space is 200x100 units.
`MainActivity` holds the splash on screen for at least 2600ms so a lap completes
even when the playlist loads instantly.

## Stack
- Kotlin + Jetpack Compose (Material 3), single-activity, no nav library
- Media3 ExoPlayer 1.4.1 (HLS module included) for playback
- Coil for channel logos
- minSdk 22 (older Fire TV Sticks), target 34

## Build
No gradle wrapper is committed. First run:
```
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Install on Firestick (sideload via ADB)
1. On the Firestick: Settings → My Fire TV → Developer Options → enable **ADB Debugging** and **Apps from Unknown Sources**.
2. Find the stick's IP: Settings → My Fire TV → About → Network.
3. From this machine (same Wi-Fi):
```
adb connect <FIRESTICK_IP>:5555   # accept the prompt on the TV
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Code map
- `data/M3uParser.kt` — parses #EXTINF attrs (tvg-id, tvg-logo, group-title)
- `data/XmltvParser.kt` — streams XMLTV programmes (handles .xml and .xml.gz)
- `data/GuideRepository.kt` — HTTP fetch + SharedPreferences for URLs
- `ui/GuideScreen.kt` — THE grid guide. Geometry constants at top
  (DP_PER_MIN, ROW_HEIGHT, WINDOW_HOURS). Timeline header + LazyColumn rows
  share one horizontal scroll state.
- `ui/GuideTheme.kt` — palette. Telus purple/green variant is commented inline.
- `player/PlayerScreen.kt` — ExoPlayer, D-pad friendly
- `MainActivity.kt` — Setup → Loading → Guide → Player state machine

## Header widgets (implemented)
- **PiP preview** (`ui/PipPreview.kt`) — muted live ExoPlayer window, 198x112dp,
  debounced 900ms so D-pad scrolling doesn't thrash the stream. Set `debounceMs`
  higher on slow networks, or gate it behind a settings toggle if it hurts perf.
- **Clock + full date** — updates every 20s
- **Weather** (`data/WeatherRepository.kt`) — Open-Meteo, free, **no API key**.
  User types a city in Setup; app geocodes it, then polls current conditions
  every 20 min. Temps are Celsius; for °F change the forecast call to add
  `&temperature_unit=fahrenheit`.

## Known gaps / roadmap (good next tasks)
1. Red "now" needle line overlaying the grid at the current time
2. Auto-scroll the grid horizontally to now on open; page left/right with FF/RW keys
3. Group/category filter row (News, Sports, Movies) from `group-title`
4. Channel logo caching + fallback initials tile
5. Favorites (long-press channel), recently watched row
6. EPG refresh on a timer; persist parsed EPG to disk for fast cold start
7. Number-pad direct channel entry
8. Detail popup on OK-press (description, "Watch" / "More times")
9. Settings toggle to disable PiP preview on low-end sticks (1GB RAM models)

## Constraints
- Keep it Compose-only; do not add tv-material unless needed
- Don't bundle any playlist URLs or content sources in the app
- The visual style mimics the *pattern* of provider guides (dark grid, blue
  focus, timeline) — never copy Bell/Telus logos, fonts, or brand assets.

## Distributing to other people
See `DISTRIBUTION.md` in the outputs folder. Summary:
1. `./gradlew assembleRelease` with a real keystore (NOT the debug key) — signing config
   snippet is in DISTRIBUTION.md. Keystore + `keystore.properties` must be gitignored.
2. Host `PrestonsPlayer.apk` + `install-page/index.html` together (Netlify/Vercel/GitHub Releases).
   Link must serve the APK as a DIRECT download — Google Drive/Dropbox preview pages break Downloader.
3. Shorten the install-page URL at https://go.aftvnews.com to get a numeric Downloader code.
4. Bump `versionCode` on every release or updates won't install over the old build.

Known blocker: Amazon's newest Vega OS Fire TV sticks cannot sideload at all. No workaround.
