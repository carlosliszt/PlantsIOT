package com.github.carlosliszt.plantsiot.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.AppApplication
import com.github.carlosliszt.plantsiot.data.FirebaseRepository
import com.github.carlosliszt.plantsiot.databinding.ActivityDashboardBinding
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.github.carlosliszt.plantsiot.mqtt.MqttManager
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
    }

    private lateinit var binding: ActivityDashboardBinding
    private val app: AppApplication
        get() = application as AppApplication
    private val firebaseRepository: FirebaseRepository
        get() = app.firebaseRepository
    private val imageExecutor = Executors.newSingleThreadExecutor()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingPhotoUri = savedInstanceState
            ?.getString(KEY_PENDING_PHOTO_URI)
            ?.let(Uri::parse)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        currentPlantName = app.plant["name"] as? String ?: currentPlantName
        currentPlantSpecies = app.plant["species"] as? String ?: currentPlantSpecies
        currentTopic = app.plant["topic"] as? String ?: currentTopic
        renderHeader()
        app.plantImage?.let(binding.ivPlantImage::setImageBitmap)
        ReadingStore(this).latest()?.let(::renderReading)
        app.addMqttListener(this)
        app.ensureMqttConnected()

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
        updatePlantHealthIcon(reading.healthScore, reading.healthStatus)
    }

    private fun renderHeader() {
        binding.dashboardText.text = "Dashboard - $currentPlantName ($currentPlantSpecies)"
    }

    private fun updatePlantHealthIcon(score: Int, status: String) {
        val normalizedStatus = status.lowercase(Locale.ROOT)
        val iconColor = when {
            normalizedStatus.contains("crít") ||
                normalizedStatus.contains("crit") ||
                normalizedStatus.contains("vermelh") ||
                score < 30 -> Color.rgb(211, 47, 47)
            normalizedStatus.contains("murch") ||
                normalizedStatus.contains("seca") ||
                score < 60 -> Color.rgb(158, 117, 85)
            normalizedStatus.contains("amarel") ||
                score < 80 -> Color.rgb(245, 180, 0)
            else -> Color.rgb(46, 125, 50)
        }
        binding.ivPlantHealth.setColorFilter(iconColor)
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
        firebaseRepository.removePlantImage { success, error ->
            runOnUiThread {
                binding.btnRemovePlantPhoto.isEnabled = true
                if (!success) {
                    Toast.makeText(
                        this,
                        error ?: "Não foi possível remover a foto.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@runOnUiThread
                }

                app.restorePlantImage { bitmap ->
                    runOnUiThread {
                        if (bitmap != null) {
                            binding.ivPlantImage.setImageBitmap(bitmap)
                        } else {
                            binding.ivPlantImage.setImageDrawable(null)
                        }
                        Toast.makeText(
                            this,
                            "Foto removida. Imagem da API restaurada.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun savePlantBitmap(bitmap: Bitmap) {
        val output = ByteArrayOutputStream()
        val maxDimension = 1280
        val scale = minOf(
            1f,
            maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
        )
        val compressedBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
        if (!compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)) {
            if (compressedBitmap !== bitmap) compressedBitmap.recycle()
            runOnUiThread {
                Toast.makeText(this, "Não foi possível preparar a foto.", Toast.LENGTH_LONG).show()
            }
            return
        }
        if (compressedBitmap !== bitmap) compressedBitmap.recycle()
        val imageBase64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        firebaseRepository.savePlantImageBase64(imageBase64) { success, error ->
            runOnUiThread {
                if (success) {
                    app.updatePlantImage(bitmap)
                    binding.ivPlantImage.setImageBitmap(bitmap)
                    Toast.makeText(this, "Foto da planta salva", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, error ?: "Não foi possível salvar a foto.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun format(value: Double): String =
        String.format(Locale.getDefault(), "%.1f", value)

    override fun onDestroy() {
        imageExecutor.shutdownNow()
        app.removeMqttListener(this)
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
