package com.github.carlosliszt.plantsiot.controller

import com.github.carlosliszt.plantsiot.data.firebase.FirebaseRepository

class AuthController(private val repository: FirebaseRepository = FirebaseRepository()) {
    data class State(val loading: Boolean = false, val success: Boolean = false, val error: String? = null)
    fun currentUser() = repository.currentUser()
    fun login(email: String, password: String, callback: (State) -> Unit) {
        callback(State(loading = true))
        repository.login(email, password) { ok, error -> callback(State(success = ok, error = error)) }
    }
    fun register(name: String, email: String, password: String, callback: (State) -> Unit) {
        callback(State(loading = true))
        repository.register(name, email, password) { ok, error -> callback(State(success = ok, error = error)) }
    }
    fun resetPassword(email: String, callback: (State) -> Unit) {
        callback(State(loading = true))
        repository.sendPasswordReset(email) { ok, error -> callback(State(success = ok, error = error)) }
    }
}
