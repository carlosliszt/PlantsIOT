package com.github.carlosliszt.plantsiot.mqtt

import android.util.Log
import com.github.carlosliszt.plantsiot.BuildConfig
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject

class MqttManager {

    private val serverUri = "ssl://f7e1734453864ccba132409bce7936b1.s1.eu.hivemq.cloud:8883"
    private val clientId = "androidPlant_" + System.currentTimeMillis()

    private var client: MqttClient? = null

    fun connectAndSubscribe(topic: String) {
        try {
            client = MqttClient(serverUri, clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                userName = BuildConfig.MQTT_USER
                password = BuildConfig.MQTT_PASS.toCharArray()
                isAutomaticReconnect = true
                isCleanSession = true
            }

            client?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "Conexão perdida", cause)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.toString() ?: return
                    saveReading(payload)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            client?.connect(options)
            client?.subscribe(topic, 1)

        } catch (e: Exception) {
            Log.e("MQTT", "Erro ao conectar", e)
        }
    }

    private fun saveReading(payload: String) {
        try {
            val json = JSONObject(payload)

            val reading = PlantReading(
                timestamp = System.currentTimeMillis(),
                heightCm = json.optDouble("heightCm", 0.0),
                healthScore = json.optInt("healthScore", 0),
                healthStatus = json.optString("healthStatus", ""),
                greenIndex = json.optDouble("greenIndex", 0.0),
                yellowIndex = json.optDouble("yellowIndex", 0.0),
                notes = json.optString("notes", "")
            )

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

            val ref = FirebaseDatabase.getInstance().reference
                .child("plants")
                .child(uid)
                .child("planta01")
                .child("readings")
                .push()

            ref.setValue(reading)

        } catch (e: Exception) {
            Log.e("MQTT", "Erro ao salvar leitura", e)
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
        } catch (e: Exception) {
            Log.e("MQTT", "Erro ao desconectar", e)
        }
    }
}