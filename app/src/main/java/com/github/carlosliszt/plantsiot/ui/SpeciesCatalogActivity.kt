package com.github.carlosliszt.plantsiot.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.R
import com.github.carlosliszt.plantsiot.databinding.ActivitySpeciesCatalogBinding
import java.util.Locale

class SpeciesCatalogActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpeciesCatalogBinding

    private val species = listOf(
        "Alecrim — Salvia rosmarinus",
        "Alface — Lactuca sativa",
        "Babosa — Aloe vera",
        "Cacto — Cactaceae",
        "Café — Coffea arabica",
        "Cenoura — Daucus carota",
        "Cebolinha — Allium fistulosum",
        "Coentro — Coriandrum sativum",
        "Couve — Brassica oleracea",
        "Feijão — Phaseolus vulgaris",
        "Girassol — Helianthus annuus",
        "Hortênsia — Hydrangea macrophylla",
        "Hortelã — Mentha spicata",
        "Lavanda — Lavandula angustifolia",
        "Manjericão — Ocimum basilicum",
        "Orquídea — Phalaenopsis",
        "Pimenteira — Capsicum annuum",
        "Salsa — Petroselinum crispum",
        "Samambaia — Nephrolepis exaltata",
        "Suculenta — Echeveria",
        "Tomateiro — Solanum lycopersicum"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpeciesCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        val initialQuery = intent.getStringExtra(PlantRegistrationActivity.EXTRA_SEARCH_QUERY).orEmpty()
        binding.etSpeciesSearch.setText(initialQuery)
        renderSpecies(initialQuery)
        binding.etSpeciesSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderSpecies(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        binding.btnCancelSpecies.setOnClickListener { finish() }
    }

    private fun renderSpecies(query: String) {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        binding.speciesList.removeAllViews()
        species
            .filter { it.lowercase(Locale.ROOT).contains(normalizedQuery) }
            .forEach { item ->
                val separator = item.indexOf(" — ")
                val scientificName = if (separator >= 0) item.substring(separator + 3) else item
                val option = TextView(this).apply {
                    text = item
                    setTextColor(ContextCompat.getColor(this@SpeciesCatalogActivity, R.color.text_dark))
                    textSize = 17f
                    gravity = Gravity.CENTER_VERTICAL
                    minHeight = 64
                    setPadding(18, 8, 18, 8)
                    setBackgroundResource(R.drawable.bg_species_option)
                    elevation = 3f
                    setOnClickListener {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(PlantRegistrationActivity.EXTRA_SPECIES, scientificName)
                        )
                        finish()
                    }
                }
                binding.speciesList.addView(
                    option,
                    ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 6
                        bottomMargin = 6
                    }
                )
            }
    }

    private fun applySystemInsets() {
        val root = binding.root
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
