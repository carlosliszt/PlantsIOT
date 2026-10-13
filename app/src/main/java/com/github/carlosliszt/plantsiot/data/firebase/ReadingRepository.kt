package com.github.carlosliszt.plantsiot.data.firebase

import com.github.carlosliszt.plantsiot.model.plant.PlantReading

class ReadingRepository(private val facade: FirebaseRepository = FirebaseRepository()) {
    fun save(reading: PlantReading) = facade.saveReading(reading)
    fun load(callback: (List<PlantReading>, String?) -> Unit) = facade.loadReadings(onResult = callback)
}
