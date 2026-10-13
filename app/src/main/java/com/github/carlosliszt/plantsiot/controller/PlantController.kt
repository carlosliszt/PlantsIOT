package com.github.carlosliszt.plantsiot.controller

import com.github.carlosliszt.plantsiot.data.firebase.FirebaseRepository

class PlantController(private val repository: FirebaseRepository = FirebaseRepository()) {
    data class State(val loading: Boolean = false, val success: Boolean = false, val error: String? = null)
    fun save(name: String, species: String, topic: String, callback: (State) -> Unit) {
        callback(State(loading = true))
        repository.savePlant(name, species, topic) { ok, error -> callback(State(success = ok, error = error)) }
    }
    fun saveImage(base64: String, callback: (State) -> Unit) {
        callback(State(loading = true))
        repository.savePlantImageBase64(base64) { ok, error -> callback(State(success = ok, error = error)) }
    }
    fun load(callback: (Map<String, Any>?, String?) -> Unit) =
        repository.loadPlant(onResult = callback)
    fun removeImage(callback: (State) -> Unit) {
        callback(State(loading = true))
        repository.removePlantImage { ok, error -> callback(State(success = ok, error = error)) }
    }
}
