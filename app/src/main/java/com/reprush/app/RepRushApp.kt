package com.reprush.app

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RepRushApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val settings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings {})
        }
        Firebase.firestore.firestoreSettings = settings
    }
}