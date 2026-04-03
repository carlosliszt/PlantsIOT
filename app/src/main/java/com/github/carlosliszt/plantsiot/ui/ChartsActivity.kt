package com.github.carlosliszt.plantsiot.ui

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.carlosliszt.plantsiot.databinding.ActivityChartsBinding
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChartsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChartsBinding
    private val readings = mutableListOf<PlantReading>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChartsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCharts()
    }

    private fun loadCharts() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance().reference
            .child("plants")
            .child(uid)
            .child("planta01")
            .child("readings")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                readings.clear()

                snapshot.children.forEach {
                    val item = it.getValue(PlantReading::class.java)
                    if (item != null) readings.add(item)
                }

                readings.sortBy { it.timestamp }

                drawLineChart()
                drawPieChart()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun drawLineChart() {
        val entries = readings.mapIndexed { index, item ->
            Entry(index.toFloat(), item.heightCm.toFloat())
        }

        val dataSet = LineDataSet(entries, "Crescimento (cm)").apply {
            lineWidth = 3f
            valueTextSize = 10f
            circleRadius = 4f
        }

        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.description.isEnabled = false
        binding.lineChart.invalidate()
    }

    private fun drawPieChart() {
        if (readings.isEmpty()) return

        val latest = readings.last()

        val entries = mutableListOf<PieEntry>()

        if (latest.greenIndex > 0)
            entries.add(PieEntry(latest.greenIndex.toFloat(), "Verde"))

        if (latest.yellowIndex > 0)
            entries.add(PieEntry(latest.yellowIndex.toFloat(), "Amarelo"))

        if (entries.isEmpty()) return

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.rgb(76, 175, 80),
                Color.rgb(255, 193, 7)
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
            description.isEnabled = false

            centerText = latest.healthStatus
            setCenterTextSize(16f)

            setEntryLabelColor(Color.BLACK)

            animateY(1000)
            invalidate()
        }
    }
}