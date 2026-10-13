package com.github.carlosliszt.plantsiot.data.firebase

import com.github.carlosliszt.plantsiot.model.plant.Plant

class PlantRepository(private val facade: FirebaseRepository = FirebaseRepository()) {
    fun savePlant(name: String, species: String, topic: String, callback: ((Boolean, String?) -> Unit)? = null) =
        facade.savePlant(name, species, topic, onComplete = callback)
    fun saveSettings(name: String, species: String, topic: String, callback: ((Boolean, String?) -> Unit)? = null) =
        facade.saveSettings(name, species, topic, onComplete = callback)
    fun hasPlant(callback: (Boolean, String?) -> Unit) = facade.hasPlant(onResult = callback)
    fun loadPlant(callback: (Plant?, String?) -> Unit) = facade.loadPlant(onResult = callback)
}
