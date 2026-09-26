package com.github.carlosliszt.plantsiot.data

import android.graphics.Bitmap
import java.util.Locale

object PlantImageCache {
    private val cache = LinkedHashMap<String, Bitmap>(8, 0.75f, true)
    private var activeAccountId: String? = null

    fun bindAccount(accountId: String?) {
        val previousId = activeAccountId
        if (previousId != null && accountId != null && previousId != accountId) {
            clearForAccount(previousId)
        }
        if (accountId == null) {
            clearAll()
        }
        activeAccountId = accountId
    }

    fun get(accountId: String?, scientificName: String): Bitmap? {
        val normalizedAccount = accountId ?: "anonymous"
        val normalizedName = normalize(scientificName)
        if (normalizedName.isEmpty()) return null
        return cache["$normalizedAccount:$normalizedName"]
    }

    fun put(accountId: String?, scientificName: String, bitmap: Bitmap) {
        val normalizedAccount = accountId ?: "anonymous"
        val normalizedName = normalize(scientificName)
        if (normalizedName.isEmpty()) return
        cache["$normalizedAccount:$normalizedName"] = bitmap
    }

    fun clearForAccount(accountId: String?) {
        val normalizedAccount = (accountId ?: "anonymous").lowercase(Locale.ROOT)
        val keysToRemove = cache.keys.filter { it.startsWith("$normalizedAccount:") }
        keysToRemove.forEach(cache::remove)
    }

    fun clearAll() {
        cache.clear()
    }

    private fun normalize(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
}
