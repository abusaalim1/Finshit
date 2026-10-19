# Finshit (Jetpack Compose)

1. Open this folder in Android Studio (Koala or newer) and let Gradle sync.
2. Fonts are bundled in app/src/main/res/font/ (Bricolage Grotesque Bold, DM Sans Regular/Bold).
3. Run on a device or emulator (minSdk 26).
4. For the in-app AI to work in local builds, set the `OPENROUTER_API_KEY` environment variable
   before syncing (CI reads it from the GitHub Actions secret of the same name).

**This repo is public.** Secrets (the OpenRouter AI key, signing keystores and their passwords)
live in GitHub Actions secrets, never in code. CI restores the keystores from
`OLD_KEYSTORE_B64` / `RELEASE_KEYSTORE_B64` before building. Note: old commits in git history
may still contain earlier keystore blobs — a full purge would need a history rewrite.

## Features
- Projects with custom folders, custom pipeline stages, and project templates (Video Project, Podcast, Minimal)
- Tasks: status (To do / Doing / Done), due dates, priorities, tags, subtasks, project links, filters, kanban board
- Pages (notes): icons, sub-pages, templates (Video Script, Ideas, Meeting), teleprompter
- Library (kind filters + custom folders), search, publish calendar, brand kit, storage view, settings
- Local data persistence — everything survives app restarts
- Real AI assistant powered by OpenRouter (`nvidia/nemotron-3.5-super-vl`): chat + in-editor writing help (hooks, polish, summarize)
- Google sign-in via Firebase Auth; glassmorphism design, spring micro-animations

## Builds
- CI builds the **release** variant (`:app:assembleRelease`) signed with the project's stable keystore.
  The release build is `debuggable=false` so it does not trigger Play Protect warnings the way debug
  builds do. Minification/shrinking stay OFF (no behavior drift).

## Firebase
- Firebase project: **finshit-app** (`app/google-services.json`).
- applicationId is **finshit.app** to match the registered Firebase app. Code namespace stays com.finshit.app (unchanged).
- This repo is the merged build: the main app UI from the first zip + the Google login screen layer from the second zip, exactly as shipped — no UI changes.

## Login (Google + Firebase)
- **Real Google sign-in is ENABLED (2026-10-19):** `Config.WEB_CLIENT_ID` holds the Web OAuth client
  (`99506755116-vg20c83nj1pihpl7majlanrkv2lsso4f.apps.googleusercontent.com`) and
  `Config.PREVIEW_UI_ONLY = false`. "Continue with Google" now goes through Credential Manager >
  Firebase Auth for real. The Android OAuth client in `google-services.json` carries the stable keystore
  fingerprint, so no further console steps are needed for this key.
- Certificate flip DONE (2026-10-19 mega round): builds now sign with the proper release certificate\n  (`CN=Finshit`, SHA-1 `B6:F2:FE:65:4E:40:C1:80:2D:DE:08:20:92:CF:84:33:10:98:7A:C5`, registered in\n  Firebase Console). The keystore lives in GitHub Actions secrets (`RELEASE_KEYSTORE_B64`),\n  restored before each build. One-time uninstall + fresh install is needed on the device because\n  the signature changed from the old key.
- Earlier history: preview mode (`PREVIEW_UI_ONLY = true`, fake 1.2s sign-in) was used until the
  Firebase Console steps were done (SHA-1 fingerprint added, Google provider enabled, Web client ID issued).
- The old signing key fingerprint (superseded, kept for reference):

```
SHA-1: 27:DC:E7:16:E2:97:27:5B:49:06:A0:55:D1:41:89:BD:E3:B4:51:F3
```

## AI (OpenRouter)
- The app talks to OpenRouter's chat completions API (`AiClient.kt`, model
  `nvidia/nemotron-3.5-super-vl`) via `HttpURLConnection` — no new dependencies.
- The API key is injected at build time into `BuildConfig.OPENROUTER_KEY` from the
  `OPENROUTER_API_KEY` env var (CI reads the GitHub Actions secret). Builds without the key
  show a friendly "AI key missing" message instead of crashing.
- Note: client-side integration means the key is embedded in the APK at runtime by design;
  it is never committed to the repo. Treat the APK as private-to-you.

## Local data
- Projects, tasks, notes, files, calendar events, brand kit and settings are saved to `finshit-data.json`
  in the app's private files dir — everything survives app restarts. Imported files are copied into the app;
  link-only imports keep a persisted read permission.
