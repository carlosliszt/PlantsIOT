package com.github.carlosliszt.plantsiot.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.R
import com.github.carlosliszt.plantsiot.data.FirebaseRepository
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.databinding.ActivityDashboardBinding
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.github.carlosliszt.plantsiot.mqtt.MqttManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity(), MqttManager.Listener {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var mqttManager: MqttManager
    private val firebaseRepository = FirebaseRepository()

    private var currentPlantName: String = "Planta"
    private var currentPlantSpecies: String = "Desconhecida"
    private var currentTopic: String = "#"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        ReadingStore(this).latest()?.let(::renderReading)

        firebaseRepository.loadPlant { plant, error ->
            if (error == null && plant != null) {
                currentPlantName = plant["name"] as? String ?: currentPlantName
                currentPlantSpecies = plant["species"] as? String ?: currentPlantSpecies
                currentTopic = plant["topic"] as? String ?: currentTopic
                renderHeader()
            }
            mqttManager = MqttManager(this, this)
            mqttManager.connectAndSubscribe(currentTopic)
        }

        binding.btnReturn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onConnectionChanged(
        state: MqttManager.ConnectionState,
        message: String
    ) {
        binding.tvConnectionStatus.text = message
        val color = when (state) {
            MqttManager.ConnectionState.CONNECTED -> R.color.green_soft
            MqttManager.ConnectionState.CONNECTING -> R.color.accent_gold
            MqttManager.ConnectionState.DISCONNECTED,
            MqttManager.ConnectionState.ERROR -> R.color.danger
        }
        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, color))
    }

    override fun onReading(reading: PlantReading) = renderReading(reading)

    @SuppressLint("SetTextI18n")
    private fun renderReading(reading: PlantReading) = with(binding) {
        renderHeader()
        tvTemperature.text = "${format(reading.temperatureC)} °C"
        tvAirHumidity.text = "${format(reading.airHumidity)} %"
        tvSoilMoisture.text = "${format(reading.soilMoisture)} %"
        tvLuminosity.text = "${format(reading.luminosity)} lux"
        tvPh.text = format(reading.ph)
        tvRgb.text = "R ${reading.red}   G ${reading.green}   B ${reading.blue}"
        tvHealthStatus.text = reading.healthStatus.ifBlank { "Aguardando análise" }
        tvHealthScore.text = "Pontuação: ${reading.healthScore}/100"
        tvHeight.text = "Altura estimada: ${format(reading.heightCm)} cm"
        tvNotes.text = reading.notes.ifBlank { "Sem observações" }
        tvSourceTopic.text = reading.sourceTopic.ifBlank { "Aguardando tópico MQTT" }
        tvRawPayload.text = reading.rawPayload.ifBlank { "Nenhuma mensagem recebida" }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        tvLastUpdate.text = if (reading.timestamp > 0) {
            "Última atualização: ${dateFormat.format(Date(reading.timestamp))}"
        } else {
            "Aguardando a primeira leitura"
        }

        viewRgbColor.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f
            setColor(Color.rgb(reading.red, reading.green, reading.blue))
            setStroke(2, ContextCompat.getColor(this@DashboardActivity, R.color.green_dark))
        }
    }

    private fun renderHeader() {
        binding.dashboardText.text = "Dashboard - $currentPlantName ($currentPlantSpecies)"
    }

    private fun format(value: Double): String =
        String.format(Locale.getDefault(), "%.1f", value)

    override fun onDestroy() {
        mqttManager.disconnect()
        super.onDestroy()
    }

    private fun applySystemInsets() {
        val root = binding.root
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
