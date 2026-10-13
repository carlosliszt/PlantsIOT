package com.github.carlosliszt.plantsiot

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.carlosliszt.plantsiot.data.firebase.FirebaseRepository
import com.github.carlosliszt.plantsiot.data.image.PlantImageCache
import com.github.carlosliszt.plantsiot.data.image.PlantImageLoader
import com.github.carlosliszt.plantsiot.data.local.ReadingStore
import com.github.carlosliszt.plantsiot.model.plant.PlantReading
import com.github.carlosliszt.plantsiot.model.image.Image
import com.github.carlosliszt.plantsiot.model.image.ImageSource
import com.github.carlosliszt.plantsiot.mqtt.MqttManager
import com.github.carlosliszt.plantsiot.controller.ReadingController
import com.github.carlosliszt.plantsiot.controller.ApplicationInitializer
import com.github.carlosliszt.plantsiot.model.plant.Plant
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.Executors

class AppApplication : Application(), MqttManager.Listener, DefaultLifecycleObserver {
    val firebaseRepository by lazy { FirebaseRepository() }
    val readingStore by lazy { ReadingStore(this) }
    var plant: Plant = Plant()
        private set
    var plantImage: Bitmap? = null
        private set
    var image: Image = Image()
        private set
    var userName: String = "Usuário"
        private set
    var initialized = false
        private set

    private val imageExecutor = Executors.newSingleThreadExecutor()
    private val imageLoader = PlantImageLoader(imageExecutor)
    private val initializer by lazy {
        ApplicationInitializer(firebaseRepository, readingStore, imageLoader)
    }
    private var mqttManager: MqttManager? = null
    private var readingController: ReadingController? = null
    private val listeners = mutableSetOf<MqttManager.Listener>()
    private var connectionState = MqttManager.ConnectionState.CONNECTING
    private var connectionMessage = "Conectando ao HiveMQ..."
    private var initializationInProgress = false
    private val pendingCallbacks = mutableListOf<(Boolean, String?) -> Unit>()

    override fun onCreate() {
        super<Application>.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        PlantImageCache.initialize(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun initialize(onComplete: (Boolean, String?) -> Unit) {
        if (initialized) {
            onComplete(true, null)
            return
        }
        pendingCallbacks += onComplete
        if (initializationInProgress) return
        initializationInProgress = true

        initializer.initialize { state, error ->
            if (state == null) {
                finishInitialization(false, error)
                return@initialize
            }
            plant = state.plant
            userName = state.userName
            plantImage = state.bitmap
            image = state.image
            mqttManager = MqttManager(this, this)
            readingController = mqttManager?.let(::ReadingController)
            readingController?.connect(plant.topic.ifBlank { "#" })
            finishInitialization(true, null)
        }
    }

    private fun finishInitialization(success: Boolean, error: String?) {
        initialized = success
        initializationInProgress = false
        val callbacks = pendingCallbacks.toList()
        pendingCallbacks.clear()
        callbacks.forEach { it(success, error) }
    }

    fun addMqttListener(listener: MqttManager.Listener) {
        listeners += listener
        listener.onConnectionChanged(connectionState, connectionMessage)
        listener.onReading(readingStore.latest() ?: PlantReading())
    }

    fun removeMqttListener(listener: MqttManager.Listener) {
        listeners -= listener
    }

    fun updatePlantImage(bitmap: Bitmap?) {
        plantImage = bitmap
        image = Image(
            url = if (bitmap == null) "" else "memory",
            imageSource = if (bitmap == null) ImageSource.API else ImageSource.USER
        )
    }

    fun updateUserName(name: String) {
        userName = name.ifBlank { "Usuário" }
    }

    fun updatePlant(name: String, species: String, topic: String) {
        plant = plant.copy(name = name, species = species, topic = topic)
    }

    fun clearSession() {
        listeners.clear()
        mqttManager?.disconnect()
        mqttManager = null
        readingController = null
        initialized = false
        initializationInProgress = false
        pendingCallbacks.clear()
        plant = Plant()
        plantImage = null
        image = Image()
        userName = "Usuário"
        connectionState = MqttManager.ConnectionState.CONNECTING
        connectionMessage = "Conectando ao HiveMQ..."
        readingStore.clear()
        PlantImageCache.bindAccount(null)
    }

    fun restorePlantImage(onComplete: (Bitmap?) -> Unit) {
        firebaseRepository.loadPlant { loadedPlant, error ->
            if (error != null || loadedPlant == null) {
                onComplete(null)
                return@loadPlant
            }
            plant = loadedPlant
            val base64 = loadedPlant.imageBase64
            val imageUrl = loadedPlant.imageUrl
            val species = loadedPlant.species
            val complete: (Bitmap?) -> Unit = { bitmap ->
                plantImage = bitmap
                image = Image(
                    url = if (bitmap == null) "" else (imageUrl ?: "api"),
                    imageSource = ImageSource.API
                )
                onComplete(bitmap)
            }
            when {
                !base64.isNullOrBlank() -> imageLoader.loadBase64(base64, complete)
                !imageUrl.isNullOrBlank() -> imageLoader.loadUrl(imageUrl, complete)
                !species.isNullOrBlank() -> imageLoader.load(species, complete)
                else -> complete(null)
            }
        }
    }

    fun ensureMqttConnected() {
        if (connectionState == MqttManager.ConnectionState.CONNECTED ||
            connectionState == MqttManager.ConnectionState.CONNECTING
        ) {
            return
        }
        readingController?.connect(plant.topic.ifBlank { "#" })
    }

    fun reconnectMqtt() {
        mqttManager?.pause()
        readingController?.connect(plant.topic.ifBlank { "#" })
    }

    override fun onStart(owner: LifecycleOwner) {
        ensureMqttConnected()
    }

    override fun onStop(owner: LifecycleOwner) {
        mqttManager?.pause()
    }

    override fun onConnectionChanged(state: MqttManager.ConnectionState, message: String) {
        connectionState = state
        connectionMessage = message
        listeners.toList().forEach { it.onConnectionChanged(state, message) }
    }

    override fun onReading(reading: PlantReading) {
        listeners.toList().forEach { it.onReading(reading) }
    }
}
