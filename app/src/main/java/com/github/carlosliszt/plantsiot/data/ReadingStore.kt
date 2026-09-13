package com.github.carlosliszt.plantsiot.data

import android.content.Context
import com.github.carlosliszt.plantsiot.model.PlantReading
import org.json.JSONArray
import org.json.JSONObject

class ReadingStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("plants_iot_readings", Context.MODE_PRIVATE)

    @Synchronized
    fun save(reading: PlantReading) {
        val items = getAll().toMutableList()
        items.add(reading)
        val trimmed = items.takeLast(MAX_READINGS)
        val array = JSONArray()
        trimmed.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_READINGS, array.toString()).apply()
    }

    @Synchronized
    fun getAll(): List<PlantReading> {
        val raw = preferences.getString(KEY_READINGS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toReading())
                }
            }
        }.getOrDefault(emptyList())
    }

    fun latest(): PlantReading? = getAll().maxByOrNull { it.timestamp }

    private fun PlantReading.toJson() = JSONObject().apply {
        put("timestamp", timestamp)
        put("temperatureC", temperatureC)
        put("airHumidity", airHumidity)
        put("soilMoisture", soilMoisture)
        put("luminosity", luminosity)
        put("ph", ph)
        put("red", red)
        put("green", green)
        put("blue", blue)
        put("heightCm", heightCm)
        put("healthScore", healthScore)
        put("healthStatus", healthStatus)
        put("greenIndex", greenIndex)
        put("yellowIndex", yellowIndex)
        put("notes", notes)
        put("sourceTopic", sourceTopic)
        put("rawPayload", rawPayload)
    }

    private fun JSONObject.toReading() = PlantReading(
        timestamp = optLong("timestamp"),
        temperatureC = optDouble("temperatureC"),
        airHumidity = optDouble("airHumidity"),
        soilMoisture = optDouble("soilMoisture"),
        luminosity = optDouble("luminosity"),
        ph = optDouble("ph"),
        red = optInt("red"),
        green = optInt("green"),
        blue = optInt("blue"),
        heightCm = optDouble("heightCm"),
        healthScore = optInt("healthScore"),
        healthStatus = optString("healthStatus"),
        greenIndex = optDouble("greenIndex"),
        yellowIndex = optDouble("yellowIndex"),
        notes = optString("notes"),
        sourceTopic = optString("sourceTopic"),
        rawPayload = optString("rawPayload")
    )

    private companion object {
        const val KEY_READINGS = "readings"
        const val MAX_READINGS = 200
    }
}
