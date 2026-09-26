package com.github.carlosliszt.plantsiot.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.carlosliszt.plantsiot.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ExecutorService

class PlantImageLoader(private val executor: ExecutorService) {

    fun load(scientificName: String, onResult: (Bitmap?) -> Unit) {
        val normalizedName = normalize(scientificName)
        if (normalizedName.isEmpty()) {
            onResult(null)
            return
        }

        val accountId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        PlantImageCache.get(accountId, normalizedName)?.let { cached ->
            onResult(cached)
            return
        }

        executor.execute {
            val bitmap = try {
                val endpoint = "https://trefle.io/api/v1/plants/search?q=" +
                    URLEncoder.encode(scientificName.trim(), Charsets.UTF_8.name())
                val imageUrl = requestImageUrl(endpoint, normalizedName)
                imageUrl?.let(::downloadBitmap)
            } catch (_: java.io.IOException) {
                null
            } catch (_: org.json.JSONException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }

            if (bitmap != null) {
                PlantImageCache.put(accountId, normalizedName, bitmap)
            }
            onResult(bitmap)
        }
    }

    private fun requestImageUrl(endpoint: String, scientificName: String): String? {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer ${BuildConfig.TREFLE_API_TOKEN}")
        }

        return try {
            if (connection.responseCode !in 200..299) {
                return null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val plants = JSONObject(body).optJSONArray("data") ?: return null
            for (index in 0 until plants.length()) {
                val plant = plants.optJSONObject(index) ?: continue
                if (normalize(plant.optString("scientific_name")) == scientificName) {
                    return plant.optString("image_url").takeIf { it.startsWith("https://") }
                }
            }
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadBitmap(imageUrl: String): Bitmap? {
        val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer ${BuildConfig.TREFLE_API_TOKEN}")
        }
        return try {
            if (connection.responseCode !in 200..299) {
                return null
            }
            connection.inputStream.use(BitmapFactory::decodeStream)
        } finally {
            connection.disconnect()
        }
    }

    private fun normalize(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
}
