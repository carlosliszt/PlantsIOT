package com.github.carlosliszt.plantsiot.model.plant

data class Plant(
    val name: String = "",
    val species: String = "",
    val topic: String = "",
    val imageBase64: String = "",
    val imageUrl: String = ""
)
