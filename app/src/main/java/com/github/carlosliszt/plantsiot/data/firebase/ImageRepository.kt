package com.github.carlosliszt.plantsiot.data.firebase

class ImageRepository(private val facade: FirebaseRepository = FirebaseRepository()) {
    fun saveBase64(value: String, callback: (Boolean, String?) -> Unit) =
        facade.savePlantImageBase64(value, onComplete = callback)
    fun remove(callback: (Boolean, String?) -> Unit) = facade.removePlantImage(onComplete = callback)
}
