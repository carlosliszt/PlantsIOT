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

class SensorsDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySensorsDetailsBinding
    private val readings = mutableListOf<PlantReading>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensorsDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()
        val reading = ReadingStore(this).latest()
        binding.tvSensorsDetails.text = reading?.let {
            String.format(Locale.getDefault(), "Temperatura: %.1f °C\nUmidade do ar: %.1f %%\nUmidade do solo: %.1f %%\nLuminosidade: %.1f lux\npH: %.1f", it.temperatureC, it.airHumidity, it.soilMoisture, it.luminosity, it.ph)
        } ?: "Nenhuma leitura disponível."
        binding.btnReturn.setOnClickListener { finish() }
        loadCharts()
    }

    private fun loadCharts() {
        readings.clear()
        readings.addAll(ReadingStore(this).getAll().sortedBy { it.timestamp })
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
