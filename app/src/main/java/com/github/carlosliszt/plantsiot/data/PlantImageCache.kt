package com.github.carlosliszt.plantsiot.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.Locale

object PlantImageCache {
    private var cacheDirectory: File? = null
    private var activeAccountId: String? = null

    @Synchronized
    fun initialize(context: Context) {
        cacheDirectory = File(context.applicationContext.cacheDir, "plant_images").apply {
            mkdirs()
        }
    }

    @Synchronized
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

    @Synchronized
    fun get(accountId: String?, scientificName: String): Bitmap? {
        val normalizedAccount = accountId ?: return null
        if (normalizedAccount != activeAccountId) return null

        val normalizedName = normalize(scientificName)
        if (normalizedName.isEmpty()) return null
        return imageFile(normalizedAccount, normalizedName)
            ?.takeIf(File::isFile)
            ?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }

    @Synchronized
    fun put(accountId: String?, scientificName: String, bitmap: Bitmap) {
        val normalizedAccount = accountId ?: return
        if (normalizedAccount != activeAccountId) return

        val normalizedName = normalize(scientificName)
        if (normalizedName.isEmpty()) return
        val file = imageFile(normalizedAccount, normalizedName) ?: return
        file.parentFile?.mkdirs()
        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        temporaryFile.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Não foi possível salvar a imagem temporária."
            }
        }
        if (!temporaryFile.renameTo(file)) {
            temporaryFile.delete()
            error("Não foi possível finalizar o arquivo temporário da imagem.")
        }
    }

    @Synchronized
    fun clearForAccount(accountId: String?) {
        val normalizedAccount = accountId ?: return
        accountDirectory(normalizedAccount)?.deleteRecursively()
    }

    @Synchronized
    fun clearAll() {
        cacheDirectory?.deleteRecursively()
        cacheDirectory?.mkdirs()
    }

    private fun accountDirectory(accountId: String): File? =
        cacheDirectory?.let { File(it, digest(accountId)) }

    private fun imageFile(accountId: String, scientificName: String): File? =
        accountDirectory(accountId)?.let { File(it, "${digest(scientificName)}.png") }

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun normalize(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
}
