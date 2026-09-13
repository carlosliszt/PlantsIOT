package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.carlosliszt.plantsiot.adapter.ReadingAdapter
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.databinding.ActivityHistoryBinding
import com.github.carlosliszt.plantsiot.model.PlantReading

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val list = mutableListOf<PlantReading>()
    private lateinit var adapter: ReadingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemInsets()

        adapter = ReadingAdapter(list)
        binding.recyclerReadings.layoutManager = LinearLayoutManager(this)
        binding.recyclerReadings.adapter = adapter
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
        list.clear()
        list.addAll(ReadingStore(this).getAll().sortedByDescending { it.timestamp })
        adapter.notifyDataSetChanged()
    }
}
