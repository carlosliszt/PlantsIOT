package com.github.carlosliszt.plantsiot.controller

import android.graphics.Bitmap
import com.github.carlosliszt.plantsiot.data.firebase.FirebaseRepository
import com.github.carlosliszt.plantsiot.data.image.PlantImageCache
import com.github.carlosliszt.plantsiot.data.image.PlantImageLoader
import com.github.carlosliszt.plantsiot.data.local.ReadingStore
import com.github.carlosliszt.plantsiot.model.image.Image
import com.github.carlosliszt.plantsiot.model.image.ImageSource
import com.github.carlosliszt.plantsiot.model.plant.Plant
import com.google.firebase.database.FirebaseDatabase

class ApplicationInitializer(
    private val repository: FirebaseRepository,
    private val readingStore: ReadingStore,
    private val imageLoader: PlantImageLoader
) {
    data class State(
        val plant: Plant,
        val userName: String,
        val bitmap: Bitmap?,
        val image: Image
    )

    fun initialize(callback: (State?, String?) -> Unit) {
        repository.validateCurrentAccount { valid, error ->
            if (!valid) {
                callback(null, error ?: "Usuário não autenticado.")
                return@validateCurrentAccount
            }
            val user = repository.currentUser()
            if (user == null) {
                callback(null, "Usuário não autenticado.")
                return@validateCurrentAccount
            }
            PlantImageCache.bindAccount(user.uid)
            FirebaseDatabase.getInstance().reference.child("users").child(user.uid).child("name").get()
                .addOnSuccessListener { snapshot ->
                    val name = snapshot.getValue(String::class.java) ?: "Usuário"
                    loadPlant(name, callback)
                }
                .addOnFailureListener { loadPlant("Usuário", callback) }
        }
    }

    private fun loadPlant(userName: String, callback: (State?, String?) -> Unit) {
        repository.hasPlant { exists, error ->
            if (error != null || !exists) {
                callback(null, error ?: "Nenhuma planta cadastrada.")
                return@hasPlant
            }
            repository.loadPlant { plant, plantError ->
                if (plantError != null || plant == null) {
                    callback(null, plantError ?: "Não foi possível carregar a planta.")
                    return@loadPlant
                }
                repository.loadReadings { readings, readingsError ->
                    if (readingsError == null && readings.isNotEmpty()) readingStore.replaceAll(readings)
                    loadImage(plant, userName, callback)
                }
            }
        }
    }

    private fun loadImage(plant: Plant, userName: String, callback: (State?, String?) -> Unit) {
        val complete: (Bitmap?) -> Unit = { bitmap ->
            callback(
                State(
                    plant = plant,
                    userName = userName,
                    bitmap = bitmap,
                    image = Image(
                        url = if (bitmap == null) "" else "memory",
                        imageSource = if (plant.imageBase64.isNotBlank()) ImageSource.USER else ImageSource.API
                    )
                ),
                null
            )
        }
        when {
            plant.imageBase64.isNotBlank() -> imageLoader.loadBase64(plant.imageBase64, complete)
            plant.imageUrl.isNotBlank() -> imageLoader.loadUrl(plant.imageUrl, complete)
            plant.species.isNotBlank() -> imageLoader.load(plant.species, complete)
            else -> complete(null)
        }
    }
}
