package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.data.FirebaseRepository
import com.github.carlosliszt.plantsiot.data.PlantImageLoader
import com.github.carlosliszt.plantsiot.data.ReadingStore
import com.github.carlosliszt.plantsiot.databinding.ActivityMainBinding
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val firebaseRepository = FirebaseRepository()
    private val imageExecutor = Executors.newSingleThreadExecutor()
    private val plantImageLoader = PlantImageLoader(imageExecutor)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemInsets()

        firebaseRepository.bindPlantImageAccount()

        val user = firebaseRepository.currentUser()
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val uid = firebaseRepository.currentUser()!!.uid
        FirebaseDatabase.getInstance().reference.child("users").child(uid).child("name").get()
            .addOnSuccessListener {
                val name = it.getValue(String::class.java) ?: "Usuário"
                binding.tvWelcome.text = "Olá, $name"
            }

        firebaseRepository.hasPlant { hasPlant, error ->
            if (error != null || !hasPlant) {
                startActivity(Intent(this, PlantRegistrationActivity::class.java))
                finish()
                return@hasPlant
            }

            firebaseRepository.loadReadings { readings, _ ->
                if (readings.isNotEmpty()) {
                    ReadingStore(this).replaceAll(readings)
                }
            }

            binding.cardDashboard.setOnClickListener {
                startActivity(Intent(this, DashboardActivity::class.java))
            }
            firebaseRepository.loadPlant { plant, _ ->
                loadPlantImage(plant?.get("species") as? String)
            }
        }

        binding.cardHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.cardCharts.setOnClickListener {
            startActivity(Intent(this, ChartsActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun loadPlantImage(scientificName: String?) {
        if (scientificName.isNullOrBlank()) return

        plantImageLoader.load(scientificName) { bitmap ->
            runOnUiThread {
                if (isFinishing || isDestroyed || bitmap == null) return@runOnUiThread
                binding.ivPlantImage.setImageBitmap(bitmap)
                binding.ivPlantImage.visibility = View.VISIBLE
            }
        }
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

    override fun onDestroy() {
        imageExecutor.shutdownNow()
        super.onDestroy()
    }

}
