package com.github.carlosliszt.plantsiot.model

data class PlantReading(
    val timestamp: Long = 0L,
    val heightCm: Double = 0.0,
    val healthScore: Int = 0,
    val healthStatus: String = "",
    val greenIndex: Double = 0.0,
    val yellowIndex: Double = 0.0,
    val notes: String = ""
)