package com.github.carlosliszt.plantsiot.domain

import com.github.carlosliszt.plantsiot.model.plant.PlantReading
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

class ReadingParser(private val clock: () -> Long = { System.currentTimeMillis() }) {
    fun parse(topic: String, payload: String, previous: PlantReading = PlantReading()): PlantReading? {
        val clean = payload.trim()
        if (clean.isEmpty()) return null
        val json = runCatching { if (clean.startsWith("{")) JSONObject(clean) else null }.getOrNull()
        if (json?.isOfflineSignal() == true) return null
        var reading = previous.copy(timestamp = clock(), sourceTopic = topic, rawPayload = clean)
        var rgbChanged = false
        var health: String? = null
        var score: Int? = null
        if (json != null) {
            val data = json.optJSONObject("data") ?: json
            val rgb = data.optJSONObject("rgb") ?: data.optJSONObject("color") ?: data.optJSONObject("cor")
            data.double("temperatureC","temperaturaC","temperatura_c","temperature","temperatura","temp")?.let { reading = reading.copy(temperatureC = it) }
            data.double("airHumidity","humidityAir","umidadeAr","umidade_ar","humidity","umidade")?.let { reading = reading.copy(airHumidity = it) }
            data.double("soilMoisture","soilHumidity","umidadeSolo","umidade_solo","soil_moisture")?.let { reading = reading.copy(soilMoisture = it) }
            data.double("luminosity","luminosidade","light","lux")?.let { reading = reading.copy(luminosity = it) }
            data.double("ph","pH","phValue","soilPh","phSolo","ph_solo")?.let { reading = reading.copy(ph = it) }
            data.double("heightCm","alturaCm","height","altura")?.let { reading = reading.copy(heightCm = it) }
            data.double("greenIndex","indiceVerde")?.let { reading = reading.copy(greenIndex = it) }
            data.double("yellowIndex","indiceAmarelo")?.let { reading = reading.copy(yellowIndex = it) }
            val r = (rgb ?: data).int("r","R","red","redValue","vermelho")
            val g = (rgb ?: data).int("g","G","green","greenValue","verde")
            val b = (rgb ?: data).int("b","B","blue","blueValue","azul")
            if (r != null) { reading = reading.copy(red = r.coerceIn(0,255)); rgbChanged = true }
            if (g != null) { reading = reading.copy(green = g.coerceIn(0,255)); rgbChanged = true }
            if (b != null) { reading = reading.copy(blue = b.coerceIn(0,255)); rgbChanged = true }
            health = data.string("healthStatus","health","saude","saúde","status")
            score = data.int("healthScore","score","pontuacao","pontuação")
            data.string("notes","observacoes","observações")?.let { reading = reading.copy(notes = it) }
        } else {
            val normalized = topic.lowercase(Locale.ROOT)
            val value = clean.trim('"').replace(',', '.').toDoubleOrNull()
            when {
                normalized.contains("temperatur") || normalized.endsWith("/temp") -> value?.let { reading = reading.copy(temperatureC=it) }
                (normalized.contains("umidade") && normalized.contains("solo")) || (normalized.contains("soil") && normalized.contains("moist")) -> value?.let { reading = reading.copy(soilMoisture=it) }
                normalized.contains("umidade") || normalized.contains("humidity") -> value?.let { reading = reading.copy(airHumidity=it) }
                normalized.endsWith("/ph") || normalized.contains("ph_solo") -> value?.let { reading = reading.copy(ph=it) }
                normalized.contains("luminos") || normalized.endsWith("/lux") || normalized.endsWith("/light") -> value?.let { reading = reading.copy(luminosity=it) }
                normalized.endsWith("/rgb") || normalized.endsWith("/cor") || normalized.endsWith("/color") -> {
                    val channels = clean.trim('"','[',']','(',')').split(',', ';', ' ').filter { it.isNotBlank() }.mapNotNull { it.toDoubleOrNull()?.roundToInt() }
                    if (channels.size < 3) return null
                    reading = reading.copy(red=channels[0].coerceIn(0,255), green=channels[1].coerceIn(0,255), blue=channels[2].coerceIn(0,255)); rgbChanged = true
                }
                normalized.endsWith("/r") || normalized.endsWith("/red") || normalized.endsWith("/vermelho") -> value?.let { reading=reading.copy(red=it.roundToInt().coerceIn(0,255)); rgbChanged=true }
                normalized.endsWith("/g") || normalized.endsWith("/green") || normalized.endsWith("/verde") -> value?.let { reading=reading.copy(green=it.roundToInt().coerceIn(0,255)); rgbChanged=true }
                normalized.endsWith("/b") || normalized.endsWith("/blue") || normalized.endsWith("/azul") -> value?.let { reading=reading.copy(blue=it.roundToInt().coerceIn(0,255)); rgbChanged=true }
                normalized.contains("score") || normalized.contains("pontuacao") -> score=value?.roundToInt()
                normalized.contains("saude") || normalized.contains("saúde") || normalized.contains("health") || normalized.endsWith("/status") -> health=clean.trim('"')
                normalized.contains("altura") || normalized.contains("height") -> value?.let { reading=reading.copy(heightCm=it) }
                else -> return null
            }
        }
        reading = if (!health.isNullOrBlank()) reading.copy(healthStatus=health!!.trim()) else reading
        if (score != null) reading = reading.copy(healthScore=score!!.coerceIn(0,100))
        return if (rgbChanged) ReadingHealthAnalyzer.analyze(reading, score == null) else reading
    }

    private fun JSONObject.double(vararg keys: String): Double? = keys.firstNotNullOfOrNull { key ->
        if (!has(key) || isNull(key)) null else when (val v=opt(key)) {
            is Number -> v.toDouble()
            is String -> v.replace(',', '.').toDoubleOrNull()
            else -> null
        }?.takeIf { it.isFinite() }
    }
    private fun JSONObject.int(vararg keys: String) = double(*keys)?.roundToInt()
    private fun JSONObject.string(vararg keys: String): String? = keys.map { optString(it).trim() }.firstOrNull { it.isNotEmpty() }
    private fun JSONObject.isOfflineSignal(): Boolean {
        if (has("online") && opt("online") == false) return true
        val text = optString("message", optString("status", optString("payload", "")))
        if (text.equals("offline", true)) return true
        val data = optJSONObject("data") ?: return false
        return data.has("online") && data.opt("online") == false ||
            data.optString("message", data.optString("status", "")).equals("offline", true)
    }
}
