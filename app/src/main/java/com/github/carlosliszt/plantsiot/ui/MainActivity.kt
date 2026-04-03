package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.carlosliszt.plantsiot.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = auth.currentUser!!.uid

        db.child("users").child(uid).child("name").get()
            .addOnSuccessListener {
                val name = it.getValue(String::class.java) ?: "Usuário"
                binding.tvWelcome.text = "Olá, $name"
            }

        binding.cardDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        binding.cardHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.cardCharts.setOnClickListener {
            startActivity(Intent(this, ChartsActivity::class.java))
        }

        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}