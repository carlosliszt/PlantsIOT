package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.carlosliszt.plantsiot.databinding.ActivitySettingsBinding
import com.github.carlosliszt.plantsiot.model.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return@setOnClickListener

            val plant = Plant(
                name = binding.etPlantName.text.toString().trim(),
                species = binding.etPlantSpecies.text.toString().trim(),
                topic = binding.etTopic.text.toString().trim()
            )

            db.child("plants")
                .child(uid)
                .child("planta01")
                .setValue(plant)
                .addOnSuccessListener {

                    Toast.makeText(this, "Configurações salvas", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}