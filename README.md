# Finshit (Jetpack Compose)

1. Open this folder in Android Studio (Koala or newer) and let Gradle sync.
2. Fonts are bundled in app/src/main/res/font/ (Bricolage Grotesque Bold, DM Sans Regular/Bold).
3. Run on a device or emulator (minSdk 26).

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
- Earlier history: preview mode (`PREVIEW_UI_ONLY = true`, fake 1.2s sign-in) was used until the
  Firebase Console steps were done (SHA-1 fingerprint added, Google provider enabled, Web client ID issued).
- The debug APK is signed with this repo's committed stable key (`debug.keystore`, shared by CI and dev builds):

```
SHA-1: 27:DC:E7:16:E2:97:27:5B:49:06:A0:55:D1:41:89:BD:E3:B4:51:F3
```

## Local data
- Projects, tasks, notes, files, calendar events, brand kit and settings are saved to `finshit-data.json`
  in the app's private files dir — everything survives app restarts. Imported files are copied into the app;
  link-only imports keep a persisted read permission.
