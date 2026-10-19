package com.finshit.app

object Config {
    /** true = fake sign-in (UI testing, no Firebase needed). Set false once Firebase is set up. */
    const val PREVIEW_UI_ONLY = true
    /** Firebase Console > Authentication > Google > Web SDK configuration > Web client ID */
    const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
}
