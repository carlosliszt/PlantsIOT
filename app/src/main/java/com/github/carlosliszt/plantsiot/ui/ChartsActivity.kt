package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.databinding.ActivityChartsBinding
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter

class ChartsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChartsBinding
    private val readings = mutableListOf<PlantReading>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChartsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemInsets()

        binding.btnReturn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        loadCharts()
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

    private fun loadCharts() {
        readings.clear()
        readings.addAll(ReadingStore(this).getAll().sortedBy { it.timestamp })
        drawLineChart()
        drawPieChart()
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

    private fun drawPieChart() {
        if (readings.isEmpty()) return

        val latest = readings.last()

        val entries = mutableListOf<PieEntry>()

        if (latest.red > 0) entries.add(PieEntry(latest.red.toFloat(), "R"))
        if (latest.green > 0) entries.add(PieEntry(latest.green.toFloat(), "G"))
        if (latest.blue > 0) entries.add(PieEntry(latest.blue.toFloat(), "B"))

        if (entries.isEmpty()) return

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.rgb(230, 72, 72),
                Color.rgb(76, 175, 80),
                Color.rgb(66, 133, 244)
            )

            valueTextSize = 14f
            valueTextColor = Color.WHITE
            sliceSpace = 3f
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChart))
        }

        binding.pieChart.apply {
            this.data = data

            setUsePercentValues(true)
            description.isEnabled = true
            description.text = "Composição RGB"


            centerText = latest.healthStatus
            setCenterTextSize(16f)

            setEntryLabelColor(Color.BLACK)

            animateY(1000)
            invalidate()
        }
    }
}
