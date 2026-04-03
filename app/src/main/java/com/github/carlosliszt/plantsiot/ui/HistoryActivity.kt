package com.github.carlosliszt.plantsiot.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.carlosliszt.plantsiot.adapter.ReadingAdapter
import com.github.carlosliszt.plantsiot.databinding.ActivityHistoryBinding
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val list = mutableListOf<PlantReading>()
    private lateinit var adapter: ReadingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ReadingAdapter(list)

        binding.recyclerReadings.layoutManager = LinearLayoutManager(this)
        binding.recyclerReadings.adapter = adapter

        loadHistory()
    }

    private fun loadHistory() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance().reference
            .child("plants")
            .child(uid)
            .child("planta01")
            .child("readings")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()

                snapshot.children.forEach {
                    val item = it.getValue(PlantReading::class.java)
                    if (item != null) list.add(item)
                }

                list.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}