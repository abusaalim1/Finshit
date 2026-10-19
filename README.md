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
- REAL sign-in (one remaining step): in Firebase Console, enable Authentication > Google sign-in for project finshit-app,
  add your app SHA-1, paste the Web client ID (client_type 3, from google-services.json "oauth_client") into
  Config.WEB_CLIENT_ID, then set PREVIEW_UI_ONLY = false.
