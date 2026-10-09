package com.github.carlosliszt.plantsiot.model

data class Image(
    val url: String = "",
    val imageSource: ImageSource = ImageSource.API
)

enum class ImageSource {
    API,
    USER
}