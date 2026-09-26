package com.github.carlosliszt.plantsiot

import android.app.Application
import com.github.carlosliszt.plantsiot.data.PlantImageCache
import com.google.firebase.database.FirebaseDatabase

class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        PlantImageCache.initialize(this)
    }
}
