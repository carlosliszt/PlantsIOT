package com.github.carlosliszt.plantsiot.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.R
import com.github.carlosliszt.plantsiot.data.FirebaseRepository
import com.github.carlosliszt.plantsiot.data.PlantImageLoader
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.databinding.ActivityDashboardBinding
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.github.carlosliszt.plantsiot.mqtt.MqttManager
import java.util.concurrent.Executors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.ByteArrayOutputStream
import android.util.Base64

class DashboardActivity : AppCompatActivity(), MqttManager.Listener {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var mqttManager: MqttManager
    private val firebaseRepository = FirebaseRepository()
    private val imageExecutor = Executors.newSingleThreadExecutor()
    private val plantImageLoader = PlantImageLoader(imageExecutor)
    private val choosePhoto = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(::uploadPlantImage) }
    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> bitmap?.let(::uploadPlantImage) }

    private var currentPlantName: String = "Planta"
    private var currentPlantSpecies: String = "Desconhecida"
    private var currentTopic: String = "#"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        ReadingStore(this).latest()?.let(::renderReading)
        firebaseRepository.loadPlant { plant, error ->
            if (error == null && plant != null) {
                currentPlantName = plant["name"] as? String ?: currentPlantName
                currentPlantSpecies = plant["species"] as? String ?: currentPlantSpecies
                currentTopic = plant["topic"] as? String ?: currentTopic
                renderHeader()
                loadPlantImage(
                    plant["imageBase64"] as? String,
                    plant["imageUrl"] as? String,
                    currentPlantSpecies
                )
            }
            mqttManager = MqttManager(this, this)
            mqttManager.connectAndSubscribe(currentTopic)
        }

        binding.btnReturn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.btnTakePlantPhoto.setOnClickListener { takePhoto.launch(null) }
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

    private fun loadPlantImage(imageBase64: String?, imageUrl: String?, scientificName: String) {
        val onBitmap: (Bitmap?) -> Unit = { bitmap ->
            runOnUiThread {
                if (isFinishing || isDestroyed || bitmap == null) return@runOnUiThread
                binding.ivPlantImage.setImageBitmap(bitmap)
            }
        }
        if (!imageBase64.isNullOrBlank()) {
            plantImageLoader.loadBase64(imageBase64, onBitmap)
        } else if (imageUrl.isNullOrBlank()) {
            plantImageLoader.load(scientificName, onBitmap)
        } else {
            plantImageLoader.loadUrl(imageUrl, onBitmap)
        }
    }

    private fun uploadPlantImage(uri: android.net.Uri) {
        binding.ivPlantImage.setImageURI(uri)
        imageExecutor.execute {
            val bitmap = contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
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

                binding.ivPlantImage.setImageDrawable(null)
                loadPlantImage(null, null, currentPlantSpecies)
                Toast.makeText(
                    this,
                    "Foto removida. Imagem da API restaurada.",
                    Toast.LENGTH_SHORT
                ).show()
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
        if (::mqttManager.isInitialized) {
            mqttManager.disconnect()
        }
        imageExecutor.shutdownNow()
        super.onDestroy()
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
