package com.example.floodgate

import android.app.Application
import com.google.firebase.FirebaseApp

class FloodGateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        installRecoveryAppCheck()
    }
}
