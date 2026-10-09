package com.github.carlosliszt.plantsiot.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
        holder.binding.ivStatus.setColorFilter(
            statusColor(holder.binding.root.context, item.healthScore, item.healthStatus)
        )
        holder.binding.tvHeight.text = "Altura: ${item.heightCm} cm"
        holder.binding.tvScore.text = "Score: ${item.healthScore}"
        holder.binding.tvEnvironment.text =
            "Temperatura: ${item.temperatureC} °C  •  Ar: ${item.airHumidity}%\n" +
            "Solo: ${item.soilMoisture}%  •  Luz: ${item.luminosity} lux  •  pH: ${item.ph}"
        holder.binding.tvRgb.text = "RGB: ${item.red}, ${item.green}, ${item.blue}"
        holder.binding.tvNotes.text = item.notes.ifBlank { "Sem observações" }
        holder.binding.tvTopic.text = "Tópico: ${item.sourceTopic}"
        holder.binding.tvDate.text = sdf.format(Date(item.timestamp))
    }

    private fun statusColor(context: android.content.Context, score: Int, status: String): Int {
        val normalizedStatus = status.lowercase(Locale.ROOT)
        return when {
            normalizedStatus.contains("crít") ||
                normalizedStatus.contains("crit") ||
                normalizedStatus.contains("vermelh") ||
                score < 30 -> ContextCompat.getColor(
                context,
                com.github.carlosliszt.plantsiot.R.color.danger
            )
            normalizedStatus.contains("murch") ||
                normalizedStatus.contains("seca") ||
                score < 60 -> android.graphics.Color.rgb(158, 117, 85)
            normalizedStatus.contains("amarel") ||
                score < 80 -> ContextCompat.getColor(
                context,
                com.github.carlosliszt.plantsiot.R.color.accent_gold
            )
            else -> ContextCompat.getColor(
                context,
                com.github.carlosliszt.plantsiot.R.color.green_soft
            )
        }
    }

}
