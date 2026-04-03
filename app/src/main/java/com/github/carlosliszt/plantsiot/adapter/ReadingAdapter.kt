package com.github.carlosliszt.plantsiot.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.carlosliszt.plantsiot.databinding.ItemReadingBinding
import com.github.carlosliszt.plantsiot.model.PlantReading
import java.text.SimpleDateFormat
import java.util.*

class ReadingAdapter(private val items: List<PlantReading>) :
    RecyclerView.Adapter<ReadingAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemReadingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReadingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        holder.binding.tvStatus.text = item.healthStatus
        holder.binding.tvHeight.text = "Altura: ${item.heightCm} cm"
        holder.binding.tvScore.text = "Score: ${item.healthScore}"
        holder.binding.tvDate.text = sdf.format(Date(item.timestamp))
    }
}