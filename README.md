# Finshit (Jetpack Compose)

1. Open this folder in Android Studio (Koala or newer) and let Gradle sync.
2. Fonts are bundled in app/src/main/res/font/ (Bricolage Grotesque Bold, DM Sans Regular/Bold).
3. Run on a device or emulator (minSdk 26).

## Firebase
- Firebase project: **finshit-app** (`app/google-services.json`).
- applicationId is **finshit.app** to match the registered Firebase app. Code namespace stays com.finshit.app (unchanged).
- This repo is the merged build: the main app UI from the first zip + the Google login screen layer from the second zip, exactly as shipped — no UI changes.

## Login (Google + Firebase)
- UI works immediately: Config.PREVIEW_UI_ONLY = true fakes the sign-in (1.2s) so you can see the full flow.
- The debug APK is signed with this repo's committed stable key (`debug.keystore`, shared by CI and dev builds):

```
SHA-1: 27:DC:E7:16:E2:97:27:5B:49:06:A0:55:D1:41:89:BD:E3:B4:51:F3
```

- REAL sign-in steps (do once in the Firebase Console):
  1. Project settings > Your apps > Android app (`finshit.app`) > **Add fingerprint** > paste the SHA-1 above > Save.
  2. Build > Authentication > Sign-in method > **Google > Enable** > Save.
  3. Open the Google provider's **Web SDK configuration**, copy the **Web client ID**, and send it to Xharpy —
     he pastes it into `Config.WEB_CLIENT_ID` and sets `PREVIEW_UI_ONLY = false`, then rebuilds.

## Local data
- Projects, tasks, notes, files, calendar events, brand kit and settings are saved to `finshit-data.json`
  in the app's private files dir — everything survives app restarts. Imported files are copied into the app;
  link-only imports keep a persisted read permission.
