package com.github.carlosliszt.plantsiot.model.image

data class Image(
    val url: String = "",
    val imageSource: ImageSource = ImageSource.API
)
