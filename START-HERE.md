# Get your shareable link — 5 steps, ~10 minutes

Everything is pre-wired. GitHub builds and signs the APK for you, then publishes
both the APK and the install page. You never install Android Studio.

Run all commands from **this folder** (the one containing `app/` and this file).

---

## 1. Make your signing key

```bash
./make-keystore.sh
```

It asks for a password, then prints **4 secrets**. Leave that terminal open.
Back up **`prestons-player.jks`** — if you lose it you can never push an update
that installs over someone's existing copy.

## 2. Put this project on GitHub

Create a new repo at [github.com](https://github.com) (public or private — both
work), then:

```bash
git init
git add .
git commit -m "Preston's Player v1.0"
git branch -M main
git remote add origin https://github.com/YOUR_NAME/prestons-player.git
git push -u origin main
```

(The `.gitignore` already keeps your `.jks` and passwords out of the repo.)

## 3. Paste in the 4 secrets

In your repo: **Settings → Secrets and variables → Actions → New repository secret.**
Add each one the script printed, as 4 separate secrets:

`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

## 4. Turn on GitHub Pages

**Settings → Pages → Source → GitHub Actions.**

## 5. Ship it

```bash
git tag v1.0
git push origin v1.0
```

Watch the **Actions** tab. In ~5 minutes it will have:

* built and signed `PrestonsPlayer.apk`
* attached it to a public **Release**
* published your **install page**

Your link appears in the **deploy-pages** job and looks like:
`https://YOUR_NAME.github.io/prestons-player/`

---

## 6. (Optional but worth it) Make it a 6-digit code

Typing that URL on a TV remote is painful. Go to
[go.aftvnews.com](https://go.aftvnews.com), paste your link, and it gives you a
numeric code. Then all you have to text someone:

> Install **Downloader** from the Amazon Appstore, open it, type **123456**.
> Then paste in your playlist URL.

To show that code on the page itself, edit the `000000` in
`install-page/index.html`, then re-run step 5 with a new tag.

*(Won't work on the newest Vega-OS Firesticks — those block sideloading entirely.)*

---

## Shipping an update later

Bump `versionCode` in `app/build.gradle.kts` (`1` → `2`), then:

```bash
git tag v1.1 && git push origin v1.1
```

Same link, new build. **Skip the version bump and it won't install over the old one.**

---

## If a build fails

Open the failed run in the **Actions** tab and read the red step:

* **"KEYSTORE_BASE64 secret is missing"** — you skipped step 3, or a secret name is misspelled. They must match exactly.
* **Pages deploy fails on the first run** — you skipped step 4. Turn on Pages (Source → GitHub Actions), then re-push the tag: `git tag -f v1.0 && git push -f origin v1.0`.
* **`keytool not found`** in step 1 — install a JDK: `brew install --cask temurin`, then re-run `./make-keystore.sh`.
