package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.data.FirebaseRepository
import com.github.carlosliszt.plantsiot.databinding.ActivityMainBinding
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val firebaseRepository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemInsets()

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

            binding.cardDashboard.setOnClickListener {
                startActivity(Intent(this, DashboardActivity::class.java))
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
