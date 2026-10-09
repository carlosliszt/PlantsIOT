package com.github.carlosliszt.plantsiot.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.databinding.ActivityReadingDetailsBinding

class ReadingDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReadingDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()
        val reading = ReadingStore(this).latest()
        binding.tvReadingDetails.text = reading?.let {
            "Altura estimada: ${it.heightCm} cm\nObservações: ${it.notes.ifBlank { "Sem observações" }}\n\nTópico: ${it.sourceTopic.ifBlank { "Não informado" }}\n\nPayload bruto:\n${it.rawPayload.ifBlank { "Nenhum payload recebido" }}"
        } ?: "Nenhuma leitura disponível."
        binding.btnReturn.setOnClickListener { finish() }
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
