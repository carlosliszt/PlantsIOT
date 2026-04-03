package com.github.carlosliszt.plantsiot.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.carlosliszt.plantsiot.databinding.ActivityDashboardBinding
import com.github.carlosliszt.plantsiot.model.PlantReading
import com.github.carlosliszt.plantsiot.mqtt.MqttManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var mqttManager: MqttManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mqttManager = MqttManager()
        mqttManager.connectAndSubscribe("planta/esp32cam/analysis")

        loadLastReading()

        binding.btnReturn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttManager.disconnect()
    }

    private fun loadLastReading() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance().reference
            .child("plants")
            .child(uid)
            .child("planta01")
            .child("readings")

        ref.limitToLast(1).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (item in snapshot.children) {
                    val reading = item.getValue(PlantReading::class.java) ?: continue

                    binding.tvHealthStatus.text = reading.healthStatus
                    binding.tvHealthScore.text = "Pontuação: ${reading.healthScore}/100"
                    binding.tvHeight.text = "Altura estimada: ${reading.heightCm} cm"
                    binding.tvNotes.text = reading.notes
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })


    }
}