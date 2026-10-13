package com.github.carlosliszt.plantsiot.mqtt

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.github.carlosliszt.plantsiot.BuildConfig
import com.github.carlosliszt.plantsiot.data.firebase.FirebaseRepository
import com.github.carlosliszt.plantsiot.data.local.ReadingStore
import com.github.carlosliszt.plantsiot.model.plant.PlantReading
import com.github.carlosliszt.plantsiot.domain.ReadingParser
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory

class MqttManager(
    context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onConnectionChanged(state: ConnectionState, message: String)
        fun onReading(reading: PlantReading)
    }

    enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED, ERROR }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val networkExecutor = Executors.newSingleThreadExecutor()
    private val saveExecutor = Executors.newSingleThreadScheduledExecutor()
    private val readingStore = ReadingStore(context)
    private val firebaseRepository = FirebaseRepository()
    private val clientId = "PlantsIOT_" + UUID.randomUUID().toString().take(12)

    @Volatile
    private var client: MqttClient? = null

    @Volatile
    private var latestReading = readingStore.latest() ?: PlantReading()

    private var topicFilter = DEFAULT_TOPIC
    private var pendingSave: ScheduledFuture<*>? = null
    private var lastSavedTimestamp = latestReading.timestamp
    private val readingParser = ReadingParser()

    fun connectAndSubscribe(topic: String = DEFAULT_TOPIC) {
        topicFilter = topic.trim().ifEmpty { DEFAULT_TOPIC }
        notifyConnection(ConnectionState.CONNECTING, "Conectando ao HiveMQ...")

        networkExecutor.execute {
            try {
                val mqttClient = MqttClient(BuildConfig.MQTT_BROKER, clientId, MemoryPersistence())
                client = mqttClient

                mqttClient.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        try {
                            mqttClient.subscribe(topicFilter, 1)
                            val detail = if (reconnect) "Reconectado" else "Conectado"
                            notifyConnection(
                                ConnectionState.CONNECTED,
                                "$detail • tópico $topicFilter"
                            )
                        } catch (error: Exception) {
                            notifyConnection(ConnectionState.ERROR, "Sem permissão para $topicFilter")
                            Log.e(TAG, "Erro ao assinar tópico", error)
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        notifyConnection(ConnectionState.DISCONNECTED, "Conexão perdida; tentando novamente")
                        Log.e(TAG, "Conexão perdida", cause)
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val sourceTopic = topic ?: return
                        val payload = message?.payload?.toString(Charsets.UTF_8) ?: return
                        parseReading(sourceTopic, payload)
                            ?.takeUnless { it.healthStatus.isOfflineStatus() }
                            ?.let { reading ->
                            latestReading = reading
                            mainHandler.post { listener.onReading(reading) }
                            scheduleSave(reading)
                            }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
                })

                val options = MqttConnectOptions().apply {
                    userName = BuildConfig.MQTT_USER
                    password = BuildConfig.MQTT_PASS.toCharArray()
                    socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    isAutomaticReconnect = true
                    isCleanSession = true
                    connectionTimeout = 15
                    keepAliveInterval = 30
                }

                mqttClient.connect(options)
            } catch (error: Exception) {
                notifyConnection(ConnectionState.ERROR, friendlyError(error))
                Log.e(TAG, "Erro ao conectar ao HiveMQ", error)
            }
        }
    }

    private fun parseReading(topic: String, payload: String): PlantReading? =
        readingParser.parse(topic, payload, latestReading)

    @Synchronized
    private fun scheduleSave(reading: PlantReading) {
        if (saveExecutor.isShutdown || reading.healthStatus.isOfflineStatus()) return
        pendingSave?.cancel(false)
        pendingSave = saveExecutor.schedule({
            readingStore.save(reading)
            firebaseRepository.saveReading(reading)
            lastSavedTimestamp = reading.timestamp
        }, 900, TimeUnit.MILLISECONDS)
    }

    private fun notifyConnection(state: ConnectionState, message: String) {
        mainHandler.post { listener.onConnectionChanged(state, message) }
    }

    private fun friendlyError(error: Exception): String {
        val text = error.message.orEmpty().lowercase(Locale.ROOT)
        return when {
            "not authorized" in text || "bad user" in text -> "Usuário ou senha do HiveMQ inválidos"
            "unknownhost" in text -> "Broker não encontrado; verifique a internet"
            else -> "Falha na conexão: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    fun disconnect() {
        if (
            latestReading.timestamp > lastSavedTimestamp &&
            !latestReading.healthStatus.isOfflineStatus()
        ) {
            readingStore.save(latestReading)
            firebaseRepository.saveReading(latestReading)
            lastSavedTimestamp = latestReading.timestamp
        }
        pendingSave?.cancel(false)
        saveExecutor.shutdownNow()
        networkExecutor.execute {
            runCatching {
                client?.takeIf { it.isConnected }?.disconnect()
                client?.close()
            }.onFailure { Log.e(TAG, "Erro ao desconectar", it) }
            networkExecutor.shutdown()
        }
    }

    private fun String.isOfflineStatus(): Boolean =
        trim().equals("offline", ignoreCase = true) || trim().equals("online", ignoreCase = true)

    private companion object {
        const val TAG = "PlantsIOT-MQTT"
        const val DEFAULT_TOPIC = "#"
    }
}
