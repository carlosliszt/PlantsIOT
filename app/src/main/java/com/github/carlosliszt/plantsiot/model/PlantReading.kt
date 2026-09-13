package com.github.carlosliszt.plantsiot.model

data class PlantReading(
    val timestamp: Long = 0L,
    val temperatureC: Double = 0.0,
    val airHumidity: Double = 0.0,
    val soilMoisture: Double = 0.0,
    val luminosity: Double = 0.0,
    val ph: Double = 0.0,
    val red: Int = 0,
    val green: Int = 0,
    val blue: Int = 0,
    val heightCm: Double = 0.0,
    val healthScore: Int = 0,
    val healthStatus: String = "",
    val greenIndex: Double = 0.0,
    val yellowIndex: Double = 0.0,
    val notes: String = "",
    val sourceTopic: String = "",
    val rawPayload: String = ""
)
