package com.github.carlosliszt.plantsiot.controller

import android.graphics.Bitmap
import android.util.Base64
import com.github.carlosliszt.plantsiot.AppApplication
import com.github.carlosliszt.plantsiot.data.firebase.ImageRepository
import com.github.carlosliszt.plantsiot.mqtt.MqttManager
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor

class DashboardController(
    private val app: AppApplication,
    private val imageRepository: ImageRepository = ImageRepository(app.firebaseRepository),
    private val executor: Executor
) {
    data class ImageState(val success: Boolean, val error: String? = null, val bitmap: Bitmap? = null)

    fun start(listener: MqttManager.Listener) {
        app.addMqttListener(listener)
        app.ensureMqttConnected()
    }

    fun stop(listener: MqttManager.Listener) = app.removeMqttListener(listener)

    fun upload(bitmap: Bitmap, callback: (ImageState) -> Unit) {
        executor.execute {
            val output = ByteArrayOutputStream()
            val scale = minOf(1f, 1280f / maxOf(bitmap.width, bitmap.height))
            val compressed = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap
            val valid = compressed.compress(Bitmap.CompressFormat.JPEG, 82, output)
            if (compressed !== bitmap) compressed.recycle()
            if (!valid) {
                callback(ImageState(false, "Não foi possível preparar a foto."))
                return@execute
            }
            val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            imageRepository.saveBase64(encoded) { success, error ->
                if (success) app.updatePlantImage(bitmap)
                callback(ImageState(success, error, if (success) bitmap else null))
            }
        }
    }

    fun remove(callback: (Boolean, String?) -> Unit) {
        imageRepository.remove { success, error ->
            if (success) app.restorePlantImage { callback(true, null) }
            else callback(false, error)
        }
    }
}
