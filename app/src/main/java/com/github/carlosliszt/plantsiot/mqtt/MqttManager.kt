package com.github.carlosliszt.plantsiot.mqtt

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.github.carlosliszt.plantsiot.BuildConfig
import com.github.carlosliszt.plantsiot.data.FirebaseRepository
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.model.PlantReading
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

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
                        parseReading(sourceTopic, payload)?.let { reading ->
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

    private fun parseReading(topic: String, payload: String): PlantReading? {
        val cleanPayload = payload.trim()
        if (cleanPayload.isEmpty()) return null

        val now = System.currentTimeMillis()
        var reading = latestReading.copy(
            timestamp = now,
            sourceTopic = topic,
            rawPayload = cleanPayload
        )

        val json = runCatching {
            if (cleanPayload.startsWith("{")) JSONObject(cleanPayload) else null
        }.getOrNull()

        var rgbChanged = false
        var explicitHealth: String? = null
        var explicitScore: Int? = null

        if (json != null) {
            val data = json.optJSONObject("data") ?: json
            val rgb = data.optJSONObject("rgb")
                ?: data.optJSONObject("color")
                ?: data.optJSONObject("cor")
                ?: json.optJSONObject("rgb")

            data.firstDouble("temperatureC", "temperaturaC", "temperatura_c", "temperature", "temperatura", "temp")
                ?.let { reading = reading.copy(temperatureC = it) }
            data.firstDouble("airHumidity", "humidityAir", "umidadeAr", "umidade_ar", "humidity", "umidade")
                ?.let { reading = reading.copy(airHumidity = it) }
            data.firstDouble("soilMoisture", "soilHumidity", "umidadeSolo", "umidade_solo", "soil_moisture")
                ?.let { reading = reading.copy(soilMoisture = it) }
            data.firstDouble("luminosity", "luminosidade", "light", "lux")
                ?.let { reading = reading.copy(luminosity = it) }
            data.firstDouble("ph", "pH", "phValue", "soilPh", "phSolo", "ph_solo")
                ?.let { reading = reading.copy(ph = it) }
            data.firstDouble("heightCm", "alturaCm", "height", "altura")
                ?.let { reading = reading.copy(heightCm = it) }
            data.firstDouble("greenIndex", "indiceVerde")
                ?.let { reading = reading.copy(greenIndex = it) }
            data.firstDouble("yellowIndex", "indiceAmarelo")
                ?.let { reading = reading.copy(yellowIndex = it) }

            val red = rgb?.firstInt("r", "R", "red", "redValue", "vermelho")
                ?: data.firstInt("r", "R", "red", "redValue", "vermelho")
            val green = rgb?.firstInt("g", "G", "green", "greenValue", "verde")
                ?: data.firstInt("g", "G", "green", "greenValue", "verde")
            val blue = rgb?.firstInt("b", "B", "blue", "blueValue", "azul")
                ?: data.firstInt("b", "B", "blue", "blueValue", "azul")
            if (red != null) {
                reading = reading.copy(red = red.coerceIn(0, 255))
                rgbChanged = true
            }
            if (green != null) {
                reading = reading.copy(green = green.coerceIn(0, 255))
                rgbChanged = true
            }
            if (blue != null) {
                reading = reading.copy(blue = blue.coerceIn(0, 255))
                rgbChanged = true
            }

            explicitHealth = data.firstString("healthStatus", "health", "saude", "saúde", "status")
            explicitScore = data.firstInt("healthScore", "score", "pontuacao", "pontuação")
            data.firstString("notes", "observacoes", "observações")
                ?.let { reading = reading.copy(notes = it) }
        } else {
            val normalizedTopic = topic.lowercase(Locale.ROOT)
            val numericValue = cleanPayload.trim('"').replace(',', '.').toDoubleOrNull()

            when {
                normalizedTopic.contains("temperatur") || normalizedTopic.endsWith("/temp") ->
                    numericValue?.let { reading = reading.copy(temperatureC = it) }
                normalizedTopic.contains("umidade") && normalizedTopic.contains("solo") ||
                    normalizedTopic.contains("soil") && normalizedTopic.contains("moist") ->
                    numericValue?.let { reading = reading.copy(soilMoisture = it) }
                normalizedTopic.contains("umidade") || normalizedTopic.contains("humidity") ->
                    numericValue?.let { reading = reading.copy(airHumidity = it) }
                normalizedTopic.endsWith("/ph") || normalizedTopic.contains("ph_solo") ->
                    numericValue?.let { reading = reading.copy(ph = it) }
                normalizedTopic.contains("luminos") || normalizedTopic.endsWith("/lux") ||
                    normalizedTopic.endsWith("/light") ->
                    numericValue?.let { reading = reading.copy(luminosity = it) }
                normalizedTopic.endsWith("/rgb") || normalizedTopic.endsWith("/cor") ||
                    normalizedTopic.endsWith("/color") -> {
                    val channels = cleanPayload
                        .trim('"', '[', ']', '(', ')')
                        .split(',', ';', ' ')
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.toDoubleOrNull()?.roundToInt() }
                    if (channels.size >= 3) {
                        reading = reading.copy(
                            red = channels[0].coerceIn(0, 255),
                            green = channels[1].coerceIn(0, 255),
                            blue = channels[2].coerceIn(0, 255)
                        )
                        rgbChanged = true
                    } else return null
                }
                normalizedTopic.endsWith("/r") || normalizedTopic.endsWith("/red") ||
                    normalizedTopic.endsWith("/vermelho") -> numericValue?.let {
                    reading = reading.copy(red = it.roundToInt().coerceIn(0, 255)); rgbChanged = true
                }
                normalizedTopic.endsWith("/g") || normalizedTopic.endsWith("/green") ||
                    normalizedTopic.endsWith("/verde") -> numericValue?.let {
                    reading = reading.copy(green = it.roundToInt().coerceIn(0, 255)); rgbChanged = true
                }
                normalizedTopic.endsWith("/b") || normalizedTopic.endsWith("/blue") ||
                    normalizedTopic.endsWith("/azul") -> numericValue?.let {
                    reading = reading.copy(blue = it.roundToInt().coerceIn(0, 255)); rgbChanged = true
                }
                normalizedTopic.contains("score") || normalizedTopic.contains("pontuacao") ->
                    explicitScore = numericValue?.roundToInt()
                normalizedTopic.contains("saude") || normalizedTopic.contains("saúde") ||
                    normalizedTopic.contains("health") || normalizedTopic.endsWith("/status") ->
                    explicitHealth = cleanPayload.trim('"')
                normalizedTopic.contains("altura") || normalizedTopic.contains("height") ->
                    numericValue?.let { reading = reading.copy(heightCm = it) }
                else -> return null
            }
        }

        if (!explicitHealth.isNullOrBlank()) {
            reading = reading.copy(healthStatus = explicitHealth!!)
        } else if (rgbChanged) {
            reading = reading.copy(healthStatus = deriveHealth(reading.red, reading.green, reading.blue))
        }

        if (explicitScore != null) {
            reading = reading.copy(healthScore = explicitScore!!.coerceIn(0, 100))
        } else if (rgbChanged) {
            reading = reading.copy(healthScore = deriveHealthScore(reading.red, reading.green, reading.blue))
        }

        return reading
    }

    private fun deriveHealth(red: Int, green: Int, blue: Int): String {
        if (red + green + blue == 0) return "Aguardando leitura RGB"
        val yellowish = red > blue * 1.25 && green > blue * 1.25 &&
            abs(red - green) <= max(red, green) * 0.35
        return when {
            green >= red * 1.10 && green >= blue * 1.15 -> "Saudável"
            yellowish -> "Atenção: possível amarelamento"
            red > green * 1.20 -> "Atenção: baixa predominância de verde"
            else -> "Monitorar coloração"
        }
    }

    private fun deriveHealthScore(red: Int, green: Int, blue: Int): Int {
        if (red + green + blue == 0) return 0
        val dominance = green.toDouble() / max(1, max(red, blue)).toDouble()
        return (50 + (dominance - 0.75) * 65).roundToInt().coerceIn(0, 100)
    }

    @Synchronized
    private fun scheduleSave(reading: PlantReading) {
        if (saveExecutor.isShutdown) return
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
        if (latestReading.timestamp > lastSavedTimestamp) {
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

    private fun JSONObject.firstDouble(vararg keys: String): Double? {
        keys.forEach { key ->
            if (has(key) && !isNull(key)) {
                val value = opt(key)
                val parsed = when (value) {
                    is Number -> value.toDouble()
                    is String -> value.replace(',', '.').toDoubleOrNull()
                    else -> null
                }
                if (parsed != null && parsed.isFinite()) return parsed
            }
        }
        return null
    }

    private fun JSONObject.firstInt(vararg keys: String): Int? =
        firstDouble(*keys)?.roundToInt()

    private fun JSONObject.firstString(vararg keys: String): String? {
        keys.forEach { key ->
            if (has(key) && !isNull(key)) {
                val value = optString(key).trim()
                if (value.isNotEmpty()) return value
            }
        }
        return null
    }

    private companion object {
        const val TAG = "PlantsIOT-MQTT"
        const val DEFAULT_TOPIC = "#"
    }
}
