package com.github.carlosliszt.plantsiot.util

import android.graphics.Color
import com.github.carlosliszt.plantsiot.model.plant.PlantReading

object PlantHealth {
    fun color(reading: PlantReading): Int {
        val status = reading.healthStatus.lowercase()
        return when {
            status.contains("crít") || status.contains("crit") ||
                status.contains("vermelh") || reading.healthScore < 30 -> Color.rgb(211, 47, 47)
            status.contains("murch") || status.contains("seca") ||
                reading.healthScore < 60 -> Color.rgb(255,117,24)
            status.contains("amarel") || reading.healthScore < 80 -> Color.rgb(245, 180, 0)
            else -> Color.rgb(46, 125, 50)
        }
    }

    fun status(reading: PlantReading): String =
        reading.healthStatus.ifBlank { "Aguardando análise" }
}
