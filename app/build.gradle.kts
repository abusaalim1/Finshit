plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")  // Firebase connected: project finshit-app
}
android {
    namespace = "com.finshit.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "finshit.app"
        minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0"
        // Injected from the OPENROUTER_API_KEY env var in CI (GitHub Actions secret) —
        // the key never lives in the repo. Empty string when not provided.
        buildConfigField("String", "OPENROUTER_KEY", "\"${System.getenv("OPENROUTER_API_KEY") ?: ""}\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        // Legacy debug keystore (kept for compatibility; no longer used for signing).
        create("stable") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Proper release certificate (CN=Finshit, SHA-1 B6:F2:FE:65:4E:40:C1:80:2D:DE:08:20:92:CF:84:33:10:98:7A:C5).
        // Keystore is restored from the RELEASE_KEYSTORE_B64 Actions secret before the build;
        // its password comes from KEYSTORE_PASSWORD (repo secret FINSHIT_KEYSTORE_PASSWORD).
        create("release") {
            storeFile = rootProject.file("finshit-release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
            keyAlias = "finshit"
            keyPassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
