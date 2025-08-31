package com.example.bixi

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this)
    }

    companion object {
        private var instance: MyApp? = null

        val appContext: Context?
            get() = instance?.applicationContext
    }
}