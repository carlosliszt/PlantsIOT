package com.github.carlosliszt.plantsiot.data

import com.github.carlosliszt.plantsiot.model.PlantReading
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FirebaseRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    fun currentUser() = auth.currentUser

    fun login(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onComplete(false, "Informe e-mail e senha.")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message ?: "Não foi possível fazer login.")
                }
            }
    }

    fun register(name: String, email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            onComplete(false, "Preencha todos os campos.")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onComplete(false, task.exception?.message ?: "Erro ao criar a conta.")
                    return@addOnCompleteListener
                }

                val uid = auth.currentUser?.uid ?: run {
                    onComplete(false, "Usuário não encontrado após cadastro.")
                    return@addOnCompleteListener
                }

                val profile = mapOf(
                    "name" to name,
                    "email" to email
                )

                database.child("users").child(uid).setValue(profile)
                    .addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            onComplete(true, null)
                        } else {
                            onComplete(false, profileTask.exception?.message ?: "Conta criada, mas falha ao salvar perfil.")
                        }
                    }
            }
    }

    fun saveUserProfile(name: String, email: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete?.invoke(false, "Usuário não autenticado.")
            return
        }

        val profile = mapOf(
            "name" to name,
            "email" to email
        )

        database.child("users").child(uid).setValue(profile)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete?.invoke(true, null)
                } else {
                    onComplete?.invoke(false, task.exception?.message ?: "Falha ao atualizar perfil.")
                }
            }
    }

    fun savePlant(plantName: String, plantSpecies: String, topic: String, plantId: String = DEFAULT_PLANT_ID, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete?.invoke(false, "Usuário não autenticado.")
            return
        }

        val plantData = mapOf(
            "name" to plantName,
            "species" to plantSpecies,
            "topic" to topic,
            "readings" to emptyMap<String, Any>()
        )

        database.child("plants").child(uid).child(plantId).setValue(plantData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete?.invoke(true, null)
                } else {
                    onComplete?.invoke(false, task.exception?.message ?: "Falha ao salvar planta.")
                }
            }
    }

    fun loadPlant(plantId: String = DEFAULT_PLANT_ID, onResult: (Map<String, Any>?, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onResult(null, "Usuário não autenticado.")
            return
        }

        database.child("plants").child(uid).child(plantId)
            .get()
            .addOnSuccessListener { snapshot ->
                val value = snapshot.getValue() as? Map<*, *>?
                onResult(value?.mapKeys { it.key as String }?.mapValues { it.value as Any }, null)
            }
            .addOnFailureListener { error ->
                onResult(null, error.message ?: "Falha ao carregar planta.")
            }
    }

    fun hasPlant(plantId: String = DEFAULT_PLANT_ID, onResult: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onResult(false, "Usuário não autenticado.")
            return
        }

        database.child("plants").child(uid).child(plantId)
            .get()
            .addOnSuccessListener { snapshot ->
                val exists = snapshot.exists() && snapshot.value != null
                onResult(exists, null)
            }
            .addOnFailureListener { error ->
                onResult(false, error.message ?: "Falha ao verificar planta.")
            }
    }

    fun saveSettings(plantName: String, plantSpecies: String, topic: String, plantId: String = DEFAULT_PLANT_ID) {
        savePlant(plantName, plantSpecies, topic, plantId)
    }

    fun saveReading(reading: PlantReading, plantId: String = DEFAULT_PLANT_ID) {
        val uid = auth.currentUser?.uid ?: return
        val key = (reading.timestamp.takeIf { it > 0 } ?: System.currentTimeMillis()).toString()
        database.child("plants").child(uid).child(plantId).child("readings").child(key)
            .setValue(reading.toFirebaseMap())
    }

    fun logout() {
        auth.signOut()
    }

    companion object {
        const val DEFAULT_PLANT_ID = "planta01"
    }
}
