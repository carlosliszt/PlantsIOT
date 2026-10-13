package com.github.carlosliszt.plantsiot.domain

import com.github.carlosliszt.plantsiot.model.plant.PlantReading
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object ReadingHealthAnalyzer {
    fun analyze(reading: PlantReading, calculateScore: Boolean = true): PlantReading {
        if (reading.red + reading.green + reading.blue == 0) return reading
        return reading.copy(
            healthStatus = reading.healthStatus.ifBlank {
                when {
                    reading.green >= reading.red * 1.10 && reading.green >= reading.blue * 1.15 -> "Saudável"
                    reading.red > reading.blue * 1.25 && reading.green > reading.blue * 1.25 &&
                        abs(reading.red - reading.green) <= max(reading.red, reading.green) * 0.35 ->
                        "Atenção: possível amarelamento"
                    reading.red > reading.green * 1.20 -> "Atenção: baixa predominância de verde"
                    else -> "Monitorar coloração"
                }
            },
            healthScore = if (calculateScore) {
                val dominance = reading.green.toDouble() / max(1, max(reading.red, reading.blue)).toDouble()
                (50 + (dominance - 0.75) * 65).roundToInt().coerceIn(0, 100)
            } else reading.healthScore
        )
    }
}
