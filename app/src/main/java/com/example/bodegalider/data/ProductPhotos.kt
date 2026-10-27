package com.example.bodegalider.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.example.bodegalider.model.CloudPhoto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/**
 * Fotos de productos guardadas en Firestore (colección "productPhotos"), para que no se pierdan
 * al reinstalar la app y las vean todos los equipos. Se guardan comprimidas en JPEG (unos 100 KB)
 * y se copian a una carpeta local para no descargarlas cada vez.
 */
class ProductPhotos(context: Context) {

    private val appContext = context.applicationContext
    private val collection = FirebaseFirestore.getInstance().collection(COLLECTION)
    private val localDir = File(appContext.filesDir, "product_photos")

    /**
     * Si [imageUri] es una foto que solo está en este celular, la comprime, la sube y devuelve
     * su referencia en la nube. Si no (ya está en la nube, es del catálogo o no se puede leer),
     * devuelve [imageUri] sin cambios.
     */
    suspend fun upload(productId: String, imageUri: String): String = withContext(Dispatchers.IO) {
        if (!CloudPhoto.isLocalOnly(imageUri)) return@withContext imageUri
        val jpeg = compress(Uri.parse(imageUri)) ?: return@withContext imageUri
        val photo = CloudPhoto(productId, System.currentTimeMillis())
        saveLocally(photo, jpeg)
        // Sin señal, Firestore deja la escritura en cola y la envía al reconectar
        collection.document(productId).set(mapOf("jpeg" to Blob.fromBytes(jpeg), "version" to photo.version))
        photo.toUri()
    }

    /** Bytes JPEG de la foto: desde la copia local o, si no está, descargándola de Firestore. */
    suspend fun load(photo: CloudPhoto): ByteArray? = withContext(Dispatchers.IO) {
        val file = localFile(photo)
        if (file.exists()) return@withContext file.readBytes()
        val snapshot = runCatching { collection.document(photo.productId).get().await() }
            .onFailure { Log.w(TAG, "No se pudo descargar la foto de ${photo.productId}", it) }
            .getOrNull()
        val bytes = snapshot?.getBlob("jpeg")?.toBytes() ?: return@withContext null
        saveLocally(photo, bytes)
        bytes
    }

    fun delete(productId: String) {
        collection.document(productId).delete()
        localDir.listFiles { file -> file.name.startsWith("${productId}_") }?.forEach { it.delete() }
    }

    private fun localFile(photo: CloudPhoto) = File(localDir, "${photo.productId}_${photo.version}.jpg")

    private fun saveLocally(photo: CloudPhoto, jpeg: ByteArray) {
        localDir.mkdirs()
        // Borra versiones anteriores de la foto de este producto
        localDir.listFiles { file -> file.name.startsWith("${photo.productId}_") }?.forEach { it.delete() }
        localFile(photo).writeBytes(jpeg)
    }

    private fun compress(uri: Uri): ByteArray? = runCatching {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val rotation = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees() } ?: 0
        val bitmap = decoded.scaledAndRotated(rotation)

        var quality = 80
        var jpeg: ByteArray
        do {
            jpeg = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
            quality -= 15
        } while (jpeg.size > MAX_BYTES && quality >= 35)
        jpeg.takeIf { it.size <= MAX_BYTES }
    }.onFailure { Log.w(TAG, "No se pudo comprimir la foto $uri", it) }.getOrNull()

    private fun ExifInterface.rotationDegrees(): Int =
        when (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    /** Reduce el lado mayor a [MAX_SIDE_PX] y endereza la foto según la orientación de la cámara. */
    private fun Bitmap.scaledAndRotated(rotation: Int): Bitmap {
        val scale = minOf(1f, MAX_SIDE_PX.toFloat() / max(width, height))
        if (scale == 1f && rotation == 0) return this
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: IllegalStateException("Tarea fallida"))
            }
        }
    }

    companion object {
        private const val TAG = "ProductPhotos"
        private const val COLLECTION = "productPhotos"
        private const val MAX_SIDE_PX = 1024
        // Firestore admite documentos de hasta 1 MB
        private const val MAX_BYTES = 700_000
    }
}
