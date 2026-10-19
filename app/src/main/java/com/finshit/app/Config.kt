package com.finshit.app

object Config {
    /** true = fake sign-in (UI testing, no Firebase needed). Set false once Firebase is set up. */
    const val PREVIEW_UI_ONLY = false
    /** Firebase Console > Authentication > Google > Web SDK configuration > Web client ID */
    const val WEB_CLIENT_ID = "99506755116-vg20c83nj1pihpl7majlanrkv2lsso4f.apps.googleusercontent.com"
}
