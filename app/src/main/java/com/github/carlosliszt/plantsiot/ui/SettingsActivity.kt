package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.carlosliszt.plantsiot.databinding.ActivitySettingsBinding
import com.github.carlosliszt.plantsiot.model.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val db = FirebaseDatabase.getInstance().reference
    private var originalPlant: Plant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemInsets()

        if(FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if(FirebaseAuth.getInstance().currentUser?.uid != null) {
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            db.child("plants").child(uid).child("planta01").get()
                .addOnSuccessListener {
                    val plant = it.getValue(Plant::class.java)
                    if (plant != null) {
                        originalPlant = plant
                        binding.etPlantName.setText(plant.name)
                        binding.etPlantSpecies.setText(plant.species)
                        binding.etTopic.setText(plant.topic)
                    }
                }
        }

        binding.btnSave.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return@setOnClickListener

            val plant = Plant(
                name = binding.etPlantName.text.toString().trim(),
                species = binding.etPlantSpecies.text.toString().trim(),
                topic = binding.etTopic.text.toString().trim()
            )

            if(plant.name.isEmpty() || plant.species.isEmpty() || plant.topic.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (plant == originalPlant) {
                Toast.makeText(this, "Nenhuma alteração detectada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.child("plants")
                .child(uid)
                .child("planta01")
                .setValue(plant)
                .addOnSuccessListener {
                    Toast.makeText(this, "Configurações salvas", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        binding.btnReturn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
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