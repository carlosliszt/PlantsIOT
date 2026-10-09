package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.data.FirebaseRepository
import com.github.carlosliszt.plantsiot.AppApplication
import com.github.carlosliszt.plantsiot.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val firebaseRepository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        binding.btnSpeciesHelp.setOnClickListener {
            showHelp(
                "Nome científico",
                "Informe o nome científico da planta. Ele é usado para buscar a imagem correta da planta."
            )
        }
        binding.btnTopicHelp.setOnClickListener {
            showHelp(
                "Tópico MQTT",
                "Informe o tópico usado pelo dispositivo para publicar as leituras dos sensores. " +
                    "O símbolo # recebe mensagens de todos os tópicos."
            )
        }
        binding.btnUnknownSpecies.setOnClickListener {
            val query = binding.etPlantName.text.toString().trim()
            speciesCatalog.launch(
                Intent(this, SpeciesCatalogActivity::class.java)
                    .putExtra(PlantRegistrationActivity.EXTRA_SEARCH_QUERY, query)
            )
        }

        firebaseRepository.loadPlant { plant, error ->
            if (error != null || plant == null) {
                binding.etPlantName.setText("Planta 01")
                binding.etPlantSpecies.setText("")
                binding.etTopic.setText("#")
                return@loadPlant
            }

            binding.etPlantName.setText(plant["name"] as? String ?: "Planta 01")
            binding.etPlantSpecies.setText(plant["species"] as? String ?: "")
            binding.etTopic.setText(plant["topic"] as? String ?: "#")
        }

        binding.btnSave.setOnClickListener {
            val plantName = binding.etPlantName.text.toString().trim()
            val species = binding.etPlantSpecies.text.toString().trim()
            val topic = binding.etTopic.text.toString().trim()

            if (plantName.isEmpty() || topic.isEmpty()) {
                Toast.makeText(this, "Informe o nome da planta e o tópico MQTT", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firebaseRepository.saveSettings(plantName, species, topic)
            Toast.makeText(this, "Configurações salvas", Toast.LENGTH_SHORT).show()
        }

        binding.btnReturn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnLogout.setOnClickListener {
            (application as AppApplication).clearSession()
            firebaseRepository.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private val speciesCatalog = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data
                ?.getStringExtra(PlantRegistrationActivity.EXTRA_SPECIES)
                ?.let(binding.etPlantSpecies::setText)
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
