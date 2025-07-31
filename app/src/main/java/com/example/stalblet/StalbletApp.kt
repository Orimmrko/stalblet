package com.example.stalblet

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.android.libraries.places.api.Places

class StalbletApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!Places.isInitialized()) {
            Places.initialize(this, BuildConfig.MAPS_API_KEY)
        }
        FirebaseApp.initializeApp(this)
    }
}
