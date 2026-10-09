package com.github.carlosliszt.plantsiot.model

data class UserProfile(
    val name: String = "",
    val email: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "email" to email
    )
}