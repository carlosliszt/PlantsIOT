package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.carlosliszt.plantsiot.R
import com.github.carlosliszt.plantsiot.adapter.ReadingAdapter
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.databinding.ActivityHistoryBinding
import com.github.carlosliszt.plantsiot.model.PlantReading

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val list = mutableListOf<PlantReading>()
    private lateinit var adapter: ReadingAdapter
    private var allReadings: List<PlantReading> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemInsets()

        adapter = ReadingAdapter(list)
        binding.recyclerReadings.layoutManager = LinearLayoutManager(this)
        binding.recyclerReadings.adapter = adapter
        binding.spinnerSort.adapter = createSpinnerAdapter(
            listOf(
                "Mais recentes",
                "Mais antigas",
                "Maior score",
                "Menor score"
            )
        )
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.btnReturn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        loadHistory()
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

    private fun loadHistory() {
        allReadings = ReadingStore(this).getAll()
        val statuses = listOf("Todos") + allReadings
            .map { it.healthStatus.ifBlank { "Sem status" } }
            .distinct()
            .sorted()
        binding.spinnerStatus.adapter = createSpinnerAdapter(statuses)
        applyFilters()
    }

    private fun createSpinnerAdapter(items: List<String>): ArrayAdapter<String> =
        ArrayAdapter<String>(this, R.layout.item_history_spinner, items).apply {
            setDropDownViewResource(R.layout.item_history_spinner_dropdown)
        }

    private fun applyFilters() {
        val selectedStatus = binding.spinnerStatus.selectedItem as? String ?: "Todos"
        val filtered = allReadings
            .asSequence()
            .filter {
                selectedStatus == "Todos" ||
                    (it.healthStatus.ifBlank { "Sem status" } == selectedStatus)
            }
            .let { readings ->
                when (binding.spinnerSort.selectedItemPosition) {
                    1 -> readings.sortedBy { it.timestamp }
                    2 -> readings.sortedByDescending { it.healthScore }
                    3 -> readings.sortedBy { it.healthScore }
                    else -> readings.sortedByDescending { it.timestamp }
                }
            }
            .toList()

        list.clear()
        list.addAll(filtered)
        adapter.notifyDataSetChanged()
    }
}
