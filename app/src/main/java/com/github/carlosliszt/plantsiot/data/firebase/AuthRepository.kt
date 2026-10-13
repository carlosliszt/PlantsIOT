package com.github.carlosliszt.plantsiot.data.firebase

import com.google.firebase.auth.FirebaseUser

class AuthRepository(private val facade: FirebaseRepository = FirebaseRepository()) {
    fun currentUser(): FirebaseUser? = facade.currentUser()
    fun validateCurrentAccount(callback: (Boolean, String?) -> Unit) = facade.validateCurrentAccount(callback)
    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) =
        facade.login(email, password, callback)
    fun register(name: String, email: String, password: String, callback: (Boolean, String?) -> Unit) =
        facade.register(name, email, password, callback)
    fun sendPasswordReset(email: String, callback: (Boolean, String?) -> Unit) =
        facade.sendPasswordReset(email, callback)
    fun updateNameWithPassword(name: String, password: String, callback: (Boolean, String?) -> Unit) =
        facade.updateNameWithPassword(name, password, callback)
    fun updateEmailWithPassword(email: String, password: String, callback: (Boolean, String?) -> Unit) =
        facade.updateEmailWithPassword(email, password, callback)
    fun updatePasswordWithPassword(newPassword: String, password: String, callback: (Boolean, String?) -> Unit) =
        facade.updatePasswordWithPassword(newPassword, password, callback)
    fun logout() = facade.logout()
}
