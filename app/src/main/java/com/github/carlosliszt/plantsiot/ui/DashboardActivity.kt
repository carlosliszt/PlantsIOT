package com.github.carlosliszt.plantsiot.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.BuildConfig
import com.github.carlosliszt.plantsiot.R
import com.github.carlosliszt.plantsiot.data.local.ReadingStore
import com.github.carlosliszt.plantsiot.AppApplication
import com.github.carlosliszt.plantsiot.controller.DashboardController
import com.github.carlosliszt.plantsiot.databinding.ActivityDashboardBinding
import com.github.carlosliszt.plantsiot.model.plant.PlantReading
import com.github.carlosliszt.plantsiot.model.image.ImageSource
import com.github.carlosliszt.plantsiot.mqtt.MqttManager
import com.github.carlosliszt.plantsiot.util.PlantHealth
import com.github.carlosliszt.plantsiot.util.ReadingNotification
import java.util.concurrent.Executors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import android.util.Base64

class DashboardActivity : AppCompatActivity(), MqttManager.Listener {

    companion object {
        private const val KEY_PENDING_PHOTO_URI = "pending_photo_uri"
        private const val NOTIFICATION_PERMISSION = 7001
    }

    private lateinit var binding: ActivityDashboardBinding
    private val app: AppApplication
        get() = application as AppApplication
    private val imageExecutor = Executors.newSingleThreadExecutor()
    private val dashboardController by lazy { DashboardController(app, executor = imageExecutor) }
    private val choosePhoto = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(::uploadPlantImage) }
    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingPhotoUri
        pendingPhotoUri = null
        if (success && uri != null) {
            uploadPlantImage(uri, deleteAfterRead = true)
        } else {
            uri?.let { contentResolver.delete(it, null, null) }
        }
    }

    private var currentPlantName: String = "Planta"
    private var currentPlantSpecies: String = "Desconhecida"
    private var currentTopic: String = "#"
    private var pendingPhotoUri: Uri? = null
    private var dashboardReady = false
    private var lastNotifiedTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingPhotoUri = savedInstanceState
            ?.getString(KEY_PENDING_PHOTO_URI)
            ?.let(Uri::parse)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()
        ReadingNotification.prepare(this)
        ReadingNotification.requestPermission(this, NOTIFICATION_PERMISSION)

        currentPlantName = app.plant.name.ifBlank { currentPlantName }
        currentPlantSpecies = app.plant.species.ifBlank { currentPlantSpecies }
        currentTopic = app.plant.topic.ifBlank { currentTopic }
        renderHeader()
        app.plantImage?.let(binding.ivPlantImage::setImageBitmap)
        binding.tvApiImageNotice.visibility =
            if (app.image.imageSource == ImageSource.API) android.view.View.VISIBLE else android.view.View.GONE
        ReadingStore(this).latest()?.let {
            lastNotifiedTimestamp = it.timestamp
            renderReading(it)
        }
        dashboardController.start(this)
        dashboardReady = true

        binding.btnReturn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.btnTakePlantPhoto.setOnClickListener {
            createCameraPhotoUri()?.let { uri ->
                pendingPhotoUri = uri
                takePhoto.launch(uri)
            }
        }
        binding.btnChoosePlantPhoto.setOnClickListener { choosePhoto.launch("image/*") }
        binding.btnRemovePlantPhoto.setOnClickListener { removePlantPhoto() }

        binding.cardSensors.setOnClickListener {
            startActivity(Intent(this, SensorsDetailsActivity::class.java))
        }
        binding.cardHealth.setOnClickListener {
            startActivity(Intent(this, HealthDetailsActivity::class.java))
        }
        binding.cardReadingDetails.setOnClickListener {
            startActivity(Intent(this, ReadingDetailsActivity::class.java))
        }
    }

    override fun onConnectionChanged(
        state: MqttManager.ConnectionState,
        message: String
    ) {
        binding.tvConnectionStatus.text = message
        val color = when (state) {
            MqttManager.ConnectionState.CONNECTED -> R.color.green_soft
            MqttManager.ConnectionState.CONNECTING -> R.color.accent_gold
            MqttManager.ConnectionState.DISCONNECTED,
            MqttManager.ConnectionState.ERROR -> R.color.danger
        }
        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, color))
    }

    override fun onReading(reading: PlantReading) = renderReading(reading)

    @SuppressLint("SetTextI18n")
    private fun renderReading(reading: PlantReading) = with(binding) {
        renderHeader()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        tvLastUpdate.text = if (reading.timestamp > 0) {
            "Última atualização: ${dateFormat.format(Date(reading.timestamp))}"
        } else {
            "Aguardando a primeira leitura"
        }

        tvSensorSummary.text = "Temperatura ${format(reading.temperatureC)} °C  |  Solo ${format(reading.soilMoisture)} %"
        tvHealthSummary.text = "${reading.healthStatus.ifBlank { "Aguardando análise" }}  |  ${reading.healthScore}/100"
        tvReadingSummary.text = "Altura ${format(reading.heightCm)} cm  |  pH ${format(reading.ph)}"
        updatePlantHealthIcon(reading)
        if (dashboardReady && reading.timestamp > lastNotifiedTimestamp) {
            lastNotifiedTimestamp = reading.timestamp
            ReadingNotification.show(this@DashboardActivity, currentPlantName, reading)
        }
    }

    private fun renderHeader() {
        binding.dashboardText.text = "Dashboard - $currentPlantName ($currentPlantSpecies)"
    }

    private fun updatePlantHealthIcon(reading: PlantReading) {
        binding.ivPlantHealth.setColorFilter(PlantHealth.color(reading))
    }

    private fun uploadPlantImage(uri: Uri, deleteAfterRead: Boolean = false) {
        imageExecutor.execute {
            val bitmap = contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            if (deleteAfterRead) {
                contentResolver.delete(uri, null, null)
            }
            if (bitmap == null) {
                runOnUiThread {
                    Toast.makeText(this, "Não foi possível ler a foto.", Toast.LENGTH_LONG).show()
                }
                return@execute
            }
            savePlantBitmap(bitmap)
        }
    }

    private fun uploadPlantImage(bitmap: Bitmap) {
        savePlantBitmap(bitmap)
    }

    private fun createCameraPhotoUri(): Uri? {
        return try {
            val photoFile = File.createTempFile("plant_photo_", ".jpg", cacheDir)
            FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                photoFile
            )
        } catch (error: IOException) {
            Toast.makeText(
                this,
                "Não foi possível preparar a câmera.",
                Toast.LENGTH_LONG
            ).show()
            null
        }
    }

    private fun removePlantPhoto() {
        binding.btnRemovePlantPhoto.isEnabled = false
        dashboardController.remove { success, error ->
            runOnUiThread {
                binding.btnRemovePlantPhoto.isEnabled = true
                if (!success) {
                    Toast.makeText(this, error ?: "Não foi possível remover a foto.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                app.plantImage?.let(binding.ivPlantImage::setImageBitmap)
                    ?: binding.ivPlantImage.setImageDrawable(null)
                Toast.makeText(this, "Foto removida.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePlantBitmap(bitmap: Bitmap) {
        dashboardController.upload(bitmap) { state ->
            runOnUiThread {
                if (state.success) {
                    binding.ivPlantImage.setImageBitmap(bitmap)
                    Toast.makeText(this, "Foto da planta salva", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, state.error ?: "Não foi possível salvar a foto.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun format(value: Double): String =
        String.format(Locale.getDefault(), "%.1f", value)

    override fun onDestroy() {
        imageExecutor.shutdownNow()
        dashboardController.stop(this)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingPhotoUri?.toString()?.let { uri ->
            outState.putString(KEY_PENDING_PHOTO_URI, uri)
        }
        super.onSaveInstanceState(outState)
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
