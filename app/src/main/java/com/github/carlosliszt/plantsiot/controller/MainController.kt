package com.github.carlosliszt.plantsiot.controller

import com.github.carlosliszt.plantsiot.AppApplication

class MainController(private val app: AppApplication) {
    fun isAuthenticated() = app.firebaseRepository.currentUser() != null
    fun initialize(callback: (Boolean, String?) -> Unit) = app.initialize(callback)
    fun plantName() = app.plant.name.ifBlank { "Planta" }
}
