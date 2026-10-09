package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.data.FirebaseRepository
import com.github.carlosliszt.plantsiot.databinding.ActivityPlantRegistrationBinding

class PlantRegistrationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SPECIES = "extra_species"
        const val EXTRA_SEARCH_QUERY = "extra_search_query"
    }

    private lateinit var binding: ActivityPlantRegistrationBinding
    private val firebaseRepository = FirebaseRepository()
    private val speciesCatalog = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra(EXTRA_SPECIES)?.let(binding.etPlantSpecies::setText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        binding.btnSpeciesHelp.setOnClickListener {
            showHelp(
                "Nome científico",
                "Informe o nome científico da planta, por exemplo: Ocimum basilicum. " +
                    "Ele é usado para buscar a imagem correta da planta."
            )
        }
        binding.btnUnknownSpecies.setOnClickListener {
            val query = binding.etPlantName.text.toString().trim()
            speciesCatalog.launch(
                Intent(this, SpeciesCatalogActivity::class.java)
                    .putExtra(EXTRA_SEARCH_QUERY, query)
            )
        }
        binding.btnTopicHelp.setOnClickListener {
            showHelp(
                "Tópico MQTT",
                "Informe o tópico usado pelo seu dispositivo para publicar as leituras dos sensores. " +
                    "Exemplo: plants/minha-planta."
            )
        }

        binding.btnSavePlant.setOnClickListener {
            val plantName = binding.etPlantName.text.toString().trim()
            val species = binding.etPlantSpecies.text.toString().trim()
            val topic = binding.etTopic.text.toString().trim()

            if (plantName.isEmpty() || species.isEmpty() || topic.isEmpty()) {
                Toast.makeText(this, "Informe nome, espécie e tópico da planta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSavePlant.isEnabled = false
            firebaseRepository.savePlant(plantName, species, topic, FirebaseRepository.DEFAULT_PLANT_ID) { success, error ->
                binding.btnSavePlant.isEnabled = true
                if (!success) {
                    Toast.makeText(this, error ?: "Não foi possível salvar a planta.", Toast.LENGTH_LONG).show()
                    return@savePlant
                }

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun showHelp(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Entendi", null)
            .show()
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
