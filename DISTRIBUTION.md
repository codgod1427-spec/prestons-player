# Shipping Preston's Player to other people

Goal: one link (or one 6-digit code) you can text someone, and they install it themselves.

There are three steps. Claude Code can do steps 1 and 3 for you.

---

## Step 1 — Build a **signed release** APK

The `assembleDebug` APK works for your own stick, but don't hand it out: debug keys
are shared/insecure, and if you ever rebuild with a different key, everyone has to
uninstall before they can update. Sign it once, keep the keystore forever.

### 1a. Make a keystore (do this ONE time, then back it up)

```bash
keytool -genkey -v \
  -keystore prestons-player.jks \
  -alias prestons \
  -keyalg RSA -keysize 2048 -validity 10000
```

It asks for a password and some name/org fields (any answers are fine).

> **Back up `prestons-player.jks` and its password somewhere safe.**
> Lose it and you can never ship an update that installs over the old version —
> users would have to uninstall and lose their settings.

### 1b. Add credentials — keep them OUT of the repo

Create `keystore.properties` in the project root:

```properties
storeFile=/absolute/path/to/prestons-player.jks
storePassword=YOUR_PASSWORD
keyAlias=prestons
keyPassword=YOUR_PASSWORD
```

Add to `.gitignore`:
```
keystore.properties
*.jks
```

### 1c. Wire signing into `app/build.gradle.kts`

Add this **above** the `android { }` block:

```kotlin
import java.util.Properties
import java.io.FileInputStream

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(FileInputStream(f))
}
```

Then inside `android { }`:

```kotlin
signingConfigs {
    create("release") {
        if (keystoreProps.isNotEmpty()) {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = false          // keep false until you add ProGuard rules
    }
}
```

### 1d. Build it

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

Rename it to `PrestonsPlayer.apk` — that's the filename people will see.

**Bumping versions later:** raise `versionCode` (1 → 2 → 3) in `app/build.gradle.kts`
every release, or Android refuses to install the update over the old one.

---

## Step 2 — Host the APK somewhere public

Any of these work. Pick one:

| Option | Good for | Notes |
|---|---|---|
| **Netlify / Vercel** | Nicest result | Drop `index.html` + `PrestonsPlayer.apk` in one folder, deploy. You get a real URL and a proper landing page. |
| **GitHub Releases** | Free, permanent, versioned | Create a release, attach the APK. Direct-download link, but the URL is long and ugly to type on a TV. |
| **Your own web host** | Full control | Just make sure the APK is served as a direct download, not through a preview page. |

**Requirement:** the link must download the `.apk` file **directly.** Google Drive and
Dropbox share links fail here — they serve an HTML preview page, and Downloader chokes
on it. If you must use Drive, convert the link to its direct-download form first.

The `install-page/index.html` I built is ready to drop in. Edit two things:
- the `href` on the Download button → your APK's URL
- the `000000` in the Downloader-code box → your real code (from step 3)

---

## Step 3 — Make the short Downloader code

Typing a long URL on a TV remote is misery. Fix it:

1. On a computer, go to **https://go.aftvnews.com** (the official AFTVnews shortener —
   same developer who makes the Downloader app).
2. Paste the URL of your **install page** (or the APK directly).
3. It spits out a numeric code.

That code is what you send people: *"Open Downloader, type 123456."* Done.

---

## What to actually send someone

> Get the **Downloader** app from the Amazon Appstore, open it, and type **123456**.
> Install it, open it, and paste in your playlist URL.
> (Heads up: won't work on the newest Vega-OS Firesticks — those block sideloading.)

---

## Things that will bite you

- **Vega OS.** Amazon's newest Fire TV sticks run Vega OS and **cannot sideload at all** —
  no Downloader, no ADB APKs. Nothing you can do about it; they'd need an older Fire TV
  or an Android/Google TV box. Say this up front and save yourself the support calls.
- **"App not installed."** Almost always: they already have a copy signed with a different
  key. They uninstall first, then it works.
- **"Install unknown apps" is off.** They must enable it *for Downloader specifically*
  (Settings → Applications → Manage Installed Applications → Downloader → Allow).
- **Low storage.** Sticks fill up fast; sideloads need a couple hundred MB free.
- **Playlists aren't included.** The app ships empty by design. Each person adds their own
  M3U. Don't bundle a playlist into the APK — that turns a neutral player into a
  distribution problem, legally and ethically.
