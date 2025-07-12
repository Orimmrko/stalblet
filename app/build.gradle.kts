// File: app/build.gradle.kts

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

}

android {
    namespace   = "com.example.stalblet"
    compileSdk  = 34

    defaultConfig {
        applicationId = "com.example.stalblet"
        minSdk        = 26
        targetSdk     = 34
        versionCode   = 1
        versionName   = "1.0"
        manifestPlaceholders += mapOf(
            "MAPS_API_KEY" to (project.findProperty("MAPS_API_KEY")?.toString() ?: "")
        )
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Unit tests
    testImplementation("junit:junit:4.13.2")

    // Instrumented Android tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.1.1"))

    // Auth (Email + Phone) & UI
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")

    // Firestore & Storage
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Google Maps & Places
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.libraries.places:places:3.4.0")

    // AndroidX AppCompat (for SearchView, etc.)
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Material Components (for FloatingActionButton, theming)
    implementation("com.google.android.material:material:1.9.0")

    // Coroutines helpers (optional)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
}
apply(plugin = "com.google.gms.google-services")