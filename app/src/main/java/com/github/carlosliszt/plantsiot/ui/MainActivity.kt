package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.AppApplication
import com.github.carlosliszt.plantsiot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app: AppApplication
        get() = application as AppApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemInsets()

        if (app.firebaseRepository.currentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        app.initialize { success, error ->
            runOnUiThread {
                if (!success) {
                    if (error == "Nenhuma planta cadastrada.") {
                        startActivity(Intent(this, PlantRegistrationActivity::class.java))
                    } else {
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    finish()
                    return@runOnUiThread
                }
                binding.tvWelcome.text = "Olá, ${app.userName}"
                app.plantImage?.let {
                    binding.ivPlantImage.setImageBitmap(it)
                    binding.ivPlantImage.visibility = View.VISIBLE
                }
                binding.cardDashboard.setOnClickListener {
                    startActivity(Intent(this, DashboardActivity::class.java))
                }
                binding.cardDashboard.isEnabled = true
                hideSplash()
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

    private fun hideSplash() {
        binding.splashOverlay.animate()
            .alpha(0f)
            .setDuration(350)
            .withEndAction { binding.splashOverlay.visibility = View.GONE }
            .start()
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
