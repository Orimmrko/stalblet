package com.example.stalblet

import android.app.Application
import com.google.firebase.FirebaseApp

class StalbletApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
