package com.github.carlosliszt.plantsiot.ui

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.databinding.ActivitySensorsDetailsBinding
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.Locale

//TODO implementar melhorias de UI.
class SensorsDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySensorsDetailsBinding
    private val readings = mutableListOf<PlantReading>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensorsDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()
        val reading = ReadingStore(this).latest()
        if (reading != null) {
            binding.tvTemperature.text = String.format(Locale.getDefault(), "Temperatura: %.1f °C", reading.temperatureC)
            binding.tvAirHumidity.text = String.format(Locale.getDefault(), "Umidade do ar: %.1f %%", reading.airHumidity)
            binding.tvSoilMoisture.text = String.format(Locale.getDefault(), "Umidade do solo: %.1f %%", reading.soilMoisture)
            binding.tvLuminosity.text = String.format(Locale.getDefault(), "Luminosidade: %.1f lux", reading.luminosity)
            binding.tvPh.text = String.format(Locale.getDefault(), "pH: %.1f", reading.ph)
        } else {
            binding.tvTemperature.text = "Temperatura: Nenhuma leitura disponível."
            binding.tvAirHumidity.text = "Umidade do ar: Nenhuma leitura disponível."
            binding.tvSoilMoisture.text = "Umidade do solo: Nenhuma leitura disponível."
            binding.tvLuminosity.text = "Luminosidade: Nenhuma leitura disponível."
            binding.tvPh.text = "pH: Nenhuma leitura disponível."
        }
        binding.btnTemperatureHelp.setOnClickListener {
            showHelp("Temperatura", "Indica o calor do ambiente onde a planta está. Temperaturas muito altas ou baixas podem prejudicar o desenvolvimento.")
        }
        binding.btnAirHumidityHelp.setOnClickListener {
            showHelp("Umidade do ar", "Indica a quantidade de vapor de água no ar, em porcentagem. Ela influencia a transpiração e a hidratação da planta.")
        }
        binding.btnSoilMoistureHelp.setOnClickListener {
            showHelp("Umidade do solo", "Indica quanto de água existe no solo, em porcentagem. Use esse valor para saber se a planta pode precisar de água.")
        }
        binding.btnLuminosityHelp.setOnClickListener {
            showHelp("Luminosidade", "Mede a intensidade da luz recebida pelo sensor, em lux. A quantidade adequada depende da espécie da planta.")
        }
        binding.btnPhHelp.setOnClickListener {
            showHelp("pH do solo", "Mede se o solo é ácido ou alcalino em uma escala de 0 a 14. A maioria das plantas prefere um pH levemente ácido.")
        }
        binding.btnReturn.setOnClickListener { finish() }
        loadCharts()
    }

    private fun showHelp(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Entendi", null)
            .show()
    }

    private fun loadCharts() {
        readings.clear()
        readings.addAll(ReadingStore(this).getAll().sortedBy { it.timestamp}.takeLast(10))
        drawLineChart()
    }

    private fun drawLineChart() {
        val temperatureEntries = readings.mapIndexed { index, item ->
            Entry(index.toFloat(), item.temperatureC.toFloat())
        }
        val phEntries = readings.mapIndexed { index, item ->
            Entry(index.toFloat(), item.ph.toFloat())
        }

        val temperatureSet = LineDataSet(temperatureEntries, "Temperatura (°C)").apply {
            lineWidth = 3f
            valueTextSize = 10f
            circleRadius = 4f
            color = Color.rgb(239, 108, 68)
            setCircleColor(Color.rgb(239, 108, 68))
        }
        val phSet = LineDataSet(phEntries, "pH").apply {
            lineWidth = 3f
            valueTextSize = 10f
            circleRadius = 4f
            color = Color.rgb(47, 125, 105)
            setCircleColor(Color.rgb(47, 125, 105))
        }

        binding.lineChart.data = LineData(temperatureSet, phSet)
        binding.lineChart.description.isEnabled = false
        binding.lineChart.invalidate()
    }

    private fun applySystemInsets() {
        val root = binding.root
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialLeft + bars.left,
                initialTop + bars.top,
                initialRight + bars.right,
                initialBottom + bars.bottom )
            insets }

        ViewCompat.requestApplyInsets(root)
    }

}
