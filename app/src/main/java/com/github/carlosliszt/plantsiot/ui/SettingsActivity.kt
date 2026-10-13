package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.AppApplication
import com.github.carlosliszt.plantsiot.data.firebase.FirebaseRepository
import com.github.carlosliszt.plantsiot.controller.PlantController
import com.github.carlosliszt.plantsiot.databinding.ActivitySettingsBinding
import com.github.carlosliszt.plantsiot.model.image.ImageSource
import com.github.carlosliszt.plantsiot.R
import com.github.carlosliszt.plantsiot.util.DialogStyler

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val firebaseRepository = FirebaseRepository()
    private val plantController = PlantController(firebaseRepository)
    private var plantName = "Planta 01"
    private var plantSpecies = ""
    private var topic = "#"
    private var speciesField: EditText? = null
    private var accountName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        plantController.load { plant, error ->
            if (error == null && plant != null) {
                plantName = plant.name.ifBlank { plantName }
                plantSpecies = plant.species
                topic = plant.topic.ifBlank { topic }
            }
            renderSummaries()
        }
        accountName = firebaseRepository.currentUser()?.displayName.orEmpty()
        renderAccountSummary()

        binding.btnPlantConfig.setOnClickListener { showPlantDialog() }
        binding.btnTopicConfig.setOnClickListener { showTopicDialog() }
        binding.btnAccountConfig.setOnClickListener { showAccountDialog() }
        binding.btnReturn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.btnLogout.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Sair da conta")
                .setMessage("Deseja realmente sair?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sair") { _, _ ->
                    (application as AppApplication).clearSession()
                    firebaseRepository.logout()
                    startActivity(
                        Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }
                .create()
            dialog.setOnShowListener { DialogStyler.apply(dialog) }
            dialog.show()
        }
    }

    private fun showPlantDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_plant_config, null)
        val name = view.findViewById<EditText>(R.id.etDialogPlantName).apply { setText(plantName) }
        val species = view.findViewById<EditText>(R.id.etDialogPlantSpecies).apply {
            setText(plantSpecies)
        }.also { speciesField = it }
        view.findViewById<android.view.View>(R.id.btnDialogSpeciesHelp).setOnClickListener {
            showHelp(
                "Nome científico",
                "Informe o nome científico da planta. Ele é usado para buscar uma imagem adequada."
            )
        }
        view.findViewById<android.view.View>(R.id.btnDialogSpeciesCatalog).setOnClickListener {
            speciesField = species
            speciesCatalog.launch(
                Intent(this, SpeciesCatalogActivity::class.java)
                    .putExtra(PlantRegistrationActivity.EXTRA_SEARCH_QUERY, name.text.toString().trim())
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Configurar planta")
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()
        dialog.setOnShowListener {
            DialogStyler.apply(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = name.text.toString().trim()
                if (newName.isEmpty()) {
                    name.error = "Informe o nome da planta."
                    return@setOnClickListener
                }
                savePlant(newName, species.text.toString().trim(), dialog)
            }
        }
        dialog.show()
    }

    private fun showTopicDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_topic_config, null)
        val topicField = view.findViewById<EditText>(R.id.etDialogTopic).apply { setText(topic) }
        view.findViewById<android.view.View>(R.id.btnDialogTopicHelp).setOnClickListener {
            showHelp(
                "Tópico MQTT",
                "Informe o tópico usado pelo dispositivo para publicar as leituras. " +
                    "O símbolo # recebe mensagens de todos os tópicos permitidos."
            )
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Configurar conexão MQTT")
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()
        dialog.setOnShowListener {
            DialogStyler.apply(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newTopic = topicField.text.toString().trim()
                if (newTopic.isEmpty()) {
                    topicField.error = "Informe um tópico MQTT."
                    Toast.makeText(this, "Informe um tópico MQTT.", Toast.LENGTH_SHORT).show()
                } else {
                    topic = newTopic
                    savePlant(plantName, plantSpecies, dialog)
                }
            }
        }
        dialog.show()
    }

    private fun showAccountDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_account_actions, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Conta e segurança")
            .setView(view)
            .setNegativeButton("Fechar", null)
            .create()
        dialog.setOnShowListener { DialogStyler.apply(dialog) }
        view.findViewById<android.view.View>(R.id.btnDialogName).setOnClickListener {
            dialog.dismiss()
            showNameDialog()
        }
        view.findViewById<android.view.View>(R.id.btnDialogEmail).setOnClickListener {
            dialog.dismiss()
            showEmailDialog()
        }
        view.findViewById<android.view.View>(R.id.btnDialogPassword).setOnClickListener {
            dialog.dismiss()
            showPasswordDialog()
        }
        dialog.show()
    }

    private fun showNameDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_account_name, null)
        val name = view.findViewById<EditText>(R.id.etDialogNewName)
            .apply { setText(accountName) }
        val password = view.findViewById<EditText>(R.id.etDialogCurrentPassword)
        showAccountActionDialog("Alterar nome", view) { dialog ->
            if (name.text.toString().trim().isEmpty()) {
                name.error = "Informe o novo nome."
                return@showAccountActionDialog
            }
            firebaseRepository.updateNameWithPassword(
                name.text.toString().trim(),
                password.text.toString(),
                accountResult("Nome atualizado.", dialog)
            )
        }
    }

    private fun showEmailDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_account_email, null)
        val email = view.findViewById<EditText>(R.id.etDialogNewEmail)
            .apply { setText(firebaseRepository.currentUser()?.email.orEmpty()) }
        val password = view.findViewById<EditText>(R.id.etDialogCurrentPassword)
        showAccountActionDialog("Alterar e-mail", view) { dialog ->
            val value = email.text.toString().trim()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                email.error = "Informe um e-mail válido."
                return@showAccountActionDialog
            }
            firebaseRepository.updateEmailWithPassword(
                value,
                password.text.toString(),
                accountResult("E-mail atualizado.", dialog)
            )
        }
    }

    private fun showPasswordDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_account_password, null)
        val newPassword = view.findViewById<EditText>(R.id.etDialogNewPassword)
        val confirmation = view.findViewById<EditText>(R.id.etDialogConfirmPassword)
        val password = view.findViewById<EditText>(R.id.etDialogCurrentPassword)
        showAccountActionDialog("Alterar senha", view) { dialog ->
            val value = newPassword.text.toString()
            if (value.length < 6 || value != confirmation.text.toString()) {
                newPassword.error = "As senhas devem coincidir e ter ao menos 6 caracteres."
                return@showAccountActionDialog
            }
            firebaseRepository.updatePasswordWithPassword(
                value,
                password.text.toString(),
                accountResult("Senha atualizada.", dialog)
            )
        }
    }

    private fun showAccountActionDialog(
        title: String,
        view: android.view.View,
        action: (AlertDialog) -> Unit
    ) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar", null)
            .create()
        dialog.setOnShowListener {
            DialogStyler.apply(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                action(dialog)
            }
        }
        dialog.show()
    }

    private fun accountResult(
        successMessage: String,
        dialog: AlertDialog
    ): (Boolean, String?) -> Unit = { success, error ->
        runOnUiThread {
            if (error == null && !success) return@runOnUiThread
            Toast.makeText(
                this,
                if (success) successMessage else error ?: "Não foi possível atualizar a conta.",
                Toast.LENGTH_LONG
            ).show()
            if (success) {
                accountName = firebaseRepository.currentUser()?.displayName.orEmpty()
                (application as AppApplication).updateUserName(accountName)
                renderAccountSummary()
                dialog.dismiss()
            }
        }
    }

    private fun savePlant(name: String, species: String, dialog: AlertDialog? = null) {
        plantController.save(name, species, topic) { state ->
            runOnUiThread {
                if (state.loading) return@runOnUiThread
                if (!state.success) {
                    Toast.makeText(this, state.error ?: "Não foi possível salvar.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                plantName = name
                plantSpecies = species
                val app = application as AppApplication
                val shouldRefreshApiImage =
                    app.image.imageSource == ImageSource.API &&
                        app.plant.species != species
                app.updatePlant(
                    name = name,
                    species = species,
                    topic = topic
                )
                app.reconnectMqtt()
                val completeSave = {
                    renderSummaries()
                    dialog?.dismiss()
                    Toast.makeText(this, "Configurações salvas", Toast.LENGTH_SHORT).show()
                }
                if (shouldRefreshApiImage) {
                    app.restorePlantImage { runOnUiThread { completeSave() } }
                } else {
                    completeSave()
                }
            }
        }
    }

    private fun renderSummaries() {
        binding.tvPlantSummary.text = if (plantSpecies.isBlank()) {
            plantName
        } else {
            "$plantName • $plantSpecies"
        }
        binding.tvTopicSummary.text = topic
    }

    private fun renderAccountSummary() {
        val email = firebaseRepository.currentUser()?.email.orEmpty()
        binding.tvAccountSummary.text = listOf(accountName, email)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
            .ifBlank { "Nome, e-mail e senha" }
    }

    private val speciesCatalog = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data
                ?.getStringExtra(PlantRegistrationActivity.EXTRA_SPECIES)
                ?.let {
                    speciesField?.setText(it)
                    speciesField = null
                }
        }
    }

    private fun showHelp(title: String, message: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Entendi", null)
            .create()
        dialog.setOnShowListener { DialogStyler.apply(dialog) }
        dialog.show()
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
