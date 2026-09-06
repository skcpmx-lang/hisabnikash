package com.hisabnikash.app.data.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Local-only product image storage.
 *
 * Picked/captured images are copied into app-internal storage
 * (`filesDir/images/`). Nothing is ever uploaded, no full-resolution image is
 * kept in memory (Coil loads and downsamples), and the path stored on the
 * product row is just the file name, so records stay portable across backups.
 */
class ProductImageStore(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "images").apply { mkdirs() }

    /** Copies the picked image into internal storage; returns the stored name. */
    fun import(uri: Uri): String? = runCatching {
        val source = context.contentResolver.openInputStream(uri) ?: return null
        val name = newName()
        val dest = File(dir, name)
        source.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        name
    }.getOrNull()

    /** Copies a camera capture (already on disk) into internal storage. */
    fun importCamera(source: File): String? = runCatching {
        if (!source.exists()) return null
        val name = newName()
        val dest = File(dir, name)
        source.copyTo(dest, overwrite = true)
        source.delete()
        name
    }.getOrNull()

    /** Resolves a stored image name to a file, or null when missing. */
    fun file(path: String?): File? = path
        ?.let { p -> if (File(p).isAbsolute) File(p) else File(dir, p) }
        ?.takeIf { it.exists() }

    /** Best-effort removal of a stored image file. */
    fun delete(path: String?) {
        file(path)?.delete()
    }

    private fun newName(): String =
        "p_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
}
