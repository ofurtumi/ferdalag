# Fri Camper — companion Android app

Publishing tool for the blog: write posts, attach photos, log the GPS route,
tweak stats — everything lands as git commits in this repo, which triggers the
site deploy. No backend, no accounts, just a GitHub token.

## How it publishes

The app never runs git. It uses GitHub's Git Data API to make one atomic
commit per publish (blobs → tree → commit → branch ref), carrying the markdown
post, resized photos, `src/data/route.json` and/or `src/data/stats.json`
together. Saving a post only writes a bundle to app storage; a WorkManager job
pushes queued bundles whenever the phone has connectivity and retries with
backoff when it doesn't. Losing signal just means the queue grows.

## Prerequisites & status

- Android Studio Ladybug (2024.2) or newer — AGP 8.7.3 requires it. The
  bundled JDK is fine (needs 17+). Gradle 8.11.1 downloads via the wrapper;
  SDK 35 installs on first sync.
- Runs on any device/emulator with Android 8.0+ (minSdk 26).
- **Heads up:** this scaffold was written on a machine without an Android
  SDK, so it has never been compiled. The APIs used are boring and stable,
  but budget a few minutes for small fixes on the first sync/build.

## Setup

1. Open the `android/` folder in Android Studio (it will download the Gradle
   distribution and Android SDK bits on first sync) and run on a device.
2. On github.com: Settings → Developer settings → Fine-grained tokens →
   generate one scoped to **only this repo** with **Contents: read and write**.
3. In the app's Settings screen: repo owner, repo name, branch (`main`),
   the token, and the commit author name/email.

## What does what

- **Record route** toggle — foreground service, one GPS point per ~5 min /
  100 m (balanced-power). Points accumulate locally and ride along with the
  next publish as `src/data/route.json`. The site doesn't render the trace
  yet — it's the data source for upgrading straight route lines to real
  driven roads later.
- **New post** — title, date, place, coordinates (one tap to use GPS),
  excerpt, markdown body, photos (system photo picker → downscaled to 1600px
  JPEG, committed next to the post in a `<slug>/` folder and listed under a
  `photos:` frontmatter key so Astro optimizes them and can render them as a
  carousel or gallery), and a weather card that prefills from Open-Meteo and
  stays fully editable — what you save is what the site shows.
- **Trip stats** — loads `src/data/stats.json` from the repo, edit/add/remove
  rows, queue the update.

## Not built yet

- Markdown preview in the editor
- Editing or deleting already-published posts
- Managing target points (`src/data/targets.json`)
- Draft persistence across process death (don't write novels with the app
  in the background)
