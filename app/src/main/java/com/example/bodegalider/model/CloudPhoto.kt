package com.example.bodegalider.model

/**
 * Referencia a una foto de producto guardada en la nube (colección "productPhotos" de Firestore).
 * Se guarda en [Product.imageUri] como "cloud://productPhotos/<productId>?v=<version>";
 * la versión cambia con cada foto nueva para no mostrar una copia vieja guardada en el equipo.
 */
data class CloudPhoto(val productId: String, val version: Long) {

    fun toUri(): String = "$PREFIX$productId?v=$version"

    companion object {
        private const val PREFIX = "cloud://productPhotos/"

        fun parse(uri: String): CloudPhoto? {
            if (!uri.startsWith(PREFIX)) return null
            val rest = uri.removePrefix(PREFIX)
            val productId = rest.substringBefore('?')
            val version = rest.substringAfter("?v=", "").toLongOrNull() ?: return null
            if (productId.isEmpty() || '/' in productId) return null
            return CloudPhoto(productId, version)
        }

        /** Foto que solo existe en este celular (galería o cámara) y todavía hay que subir. */
        fun isLocalOnly(uri: String): Boolean =
            uri.startsWith("content://") || uri.startsWith("file://")
    }
}
