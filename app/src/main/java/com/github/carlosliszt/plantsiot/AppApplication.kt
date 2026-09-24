package com.github.carlosliszt.plantsiot

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}
