package com.github.carlosliszt.plantsiot

import android.app.Application
import android.graphics.Bitmap
import com.github.carlosliszt.plantsiot.data.PlantImageCache
import com.github.carlosliszt.plantsiot.data.FirebaseRepository
import com.github.carlosliszt.plantsiot.data.PlantImageLoader
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.github.carlosliszt.plantsiot.model.Image
import com.github.carlosliszt.plantsiot.model.ImageSource
import com.github.carlosliszt.plantsiot.mqtt.MqttManager
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.Executors

class AppApplication : Application(), MqttManager.Listener {
    val firebaseRepository by lazy { FirebaseRepository() }
    val readingStore by lazy { ReadingStore(this) }
    var plant: Map<String, Any> = emptyMap()
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
    private var mqttManager: MqttManager? = null
    private val listeners = mutableSetOf<MqttManager.Listener>()
    private var connectionState = MqttManager.ConnectionState.CONNECTING
    private var connectionMessage = "Conectando ao HiveMQ..."
    private var initializationInProgress = false
    private val pendingCallbacks = mutableListOf<(Boolean, String?) -> Unit>()

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        PlantImageCache.initialize(this)
    }

    fun initialize(onComplete: (Boolean, String?) -> Unit) {
        if (initialized) {
            onComplete(true, null)
            return
        }
        pendingCallbacks += onComplete
        if (initializationInProgress) return
        initializationInProgress = true

        firebaseRepository.validateCurrentAccount { accountValid, accountError ->
            if (!accountValid) {
                finishInitialization(false, accountError ?: "Usuário não autenticado.")
                return@validateCurrentAccount
            }

            val user = firebaseRepository.currentUser()
            if (user == null) {
                finishInitialization(false, "Usuário não autenticado.")
                return@validateCurrentAccount
            }
            PlantImageCache.bindAccount(user.uid)
            val database = FirebaseDatabase.getInstance().reference
            database.child("users").child(user.uid).child("name").get()
                .addOnSuccessListener { userName = it.getValue(String::class.java) ?: "Usuário" }
                .addOnFailureListener { }
            firebaseRepository.hasPlant { hasPlant, error ->
                if (error != null || !hasPlant) {
                    finishInitialization(false, error ?: "Nenhuma planta cadastrada.")
                    return@hasPlant
                }
                firebaseRepository.loadPlant { loadedPlant, plantError ->
                    if (plantError != null || loadedPlant == null) {
                        finishInitialization(false, plantError ?: "Não foi possível carregar a planta.")
                        return@loadPlant
                    }
                    plant = loadedPlant
                    firebaseRepository.loadReadings { readings, readingsError ->
                        if (readingsError == null && readings.isNotEmpty()) {
                            readingStore.replaceAll(readings)
                        }
                        loadImageAndFinish()
                    }
                }
            }
        }
    }

    private fun loadImageAndFinish() {
        val base64 = plant["imageBase64"] as? String
        val imageUrl = plant["imageUrl"] as? String
        val species = plant["species"] as? String
        val complete: (Bitmap?) -> Unit = { bitmap ->
            plantImage = bitmap
            image = Image(
                url = if (bitmap == null) "" else "memory",
                imageSource = if (!base64.isNullOrBlank()) ImageSource.USER else ImageSource.API
            )
            mqttManager = MqttManager(this, this).also {
                it.connectAndSubscribe(plant["topic"] as? String ?: "#")
            }
            finishInitialization(true, null)
        }
        when {
            !base64.isNullOrBlank() -> imageLoader.loadBase64(base64, complete)
            !imageUrl.isNullOrBlank() -> imageLoader.loadUrl(imageUrl, complete)
            !species.isNullOrBlank() -> imageLoader.load(species, complete)
            else -> complete(null)
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

    fun clearSession() {
        listeners.clear()
        mqttManager?.disconnect()
        mqttManager = null
        initialized = false
        initializationInProgress = false
        pendingCallbacks.clear()
        plant = emptyMap()
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
            val base64 = loadedPlant["imageBase64"] as? String
            val imageUrl = loadedPlant["imageUrl"] as? String
            val species = loadedPlant["species"] as? String
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
        mqttManager?.connectAndSubscribe(plant["topic"] as? String ?: "#")
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
