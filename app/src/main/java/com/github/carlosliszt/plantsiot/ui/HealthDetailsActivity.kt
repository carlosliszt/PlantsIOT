package com.github.carlosliszt.plantsiot.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.R
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.databinding.ActivityHealthDetailsBinding
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

class HealthDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHealthDetailsBinding
    private val readings = mutableListOf<PlantReading>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()
        val reading = ReadingStore(this).latest()
        binding.tvHealthDetails.text = reading?.let {
            "Status: ${it.healthStatus.ifBlank { "Aguardando análise" }}\nPontuação: ${it.healthScore}/100\n\nÍndices da análise\nVerde: ${it.greenIndex}\nAmarelo: ${it.yellowIndex}"
        } ?: "Nenhuma leitura disponível."
        if (reading != null) {
            binding.tvRedValue.text = "Vermelho (R): ${reading.red}"
            binding.tvGreenValue.text = "Verde (G): ${reading.green}"
            binding.tvBlueValue.text = "Azul (B): ${reading.blue}"
            binding.progressRed.progress = reading.red.coerceIn(0, 255)
            binding.progressGreen.progress = reading.green.coerceIn(0, 255)
            binding.progressBlue.progress = reading.blue.coerceIn(0, 255)

            binding.viewRgbColor.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f
                setColor(Color.rgb(reading.red, reading.green, reading.blue))
                setStroke(2, ContextCompat.getColor(this@HealthDetailsActivity, R.color.green_dark))
            }

        }
        binding.btnReturn.setOnClickListener { finish() }
        loadCharts()
    }

    private fun loadCharts() {
        readings.clear()
        readings.addAll(ReadingStore(this).getAll().sortedBy { it.timestamp }.takeLast(10))
        drawPieChart()
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
