package com.example.bodegalider.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudPhotoTest {

    @Test
    fun ida_y_vuelta() {
        val photo = CloudPhoto("pg_7802820005004", 1792977869948)
        assertEquals("cloud://productPhotos/pg_7802820005004?v=1792977869948", photo.toUri())
        assertEquals(photo, CloudPhoto.parse(photo.toUri()))
    }

    @Test
    fun rechazaUrisQueNoSonDeLaNube() {
        assertNull(CloudPhoto.parse(""))
        assertNull(CloudPhoto.parse("content://media/external/images/1"))
        assertNull(CloudPhoto.parse("android.resource://com.example.bodegalider/drawable/pantene"))
        assertNull(CloudPhoto.parse("cloud://productPhotos/abc"))
        assertNull(CloudPhoto.parse("cloud://productPhotos/?v=1"))
        assertNull(CloudPhoto.parse("cloud://productPhotos/a/b?v=1"))
    }

    @Test
    fun detectaFotosQueSoloEstanEnElCelular() {
        assertTrue(CloudPhoto.isLocalOnly("content://com.example.bodegalider.fileprovider/cache/camera/foto.jpg"))
        assertTrue(CloudPhoto.isLocalOnly("file:///data/foto.jpg"))
        assertFalse(CloudPhoto.isLocalOnly("android.resource://com.example.bodegalider/drawable/pantene"))
        assertFalse(CloudPhoto.isLocalOnly(CloudPhoto("x", 1).toUri()))
        assertFalse(CloudPhoto.isLocalOnly(""))
    }
}
