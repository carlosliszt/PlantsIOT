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
) {
    fun toFirebaseMap(): Map<String, Any> = mapOf(
        "timestamp" to timestamp,
        "temperatureC" to temperatureC,
        "airHumidity" to airHumidity,
        "soilMoisture" to soilMoisture,
        "luminosity" to luminosity,
        "ph" to ph,
        "red" to red,
        "green" to green,
        "blue" to blue,
        "heightCm" to heightCm,
        "healthScore" to healthScore,
        "healthStatus" to healthStatus,
        "greenIndex" to greenIndex,
        "yellowIndex" to yellowIndex,
        "notes" to notes,
        "sourceTopic" to sourceTopic,
        "rawPayload" to rawPayload
    )
}
