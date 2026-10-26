package com.example.bodegalider.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeEncoderTest {

    @Test
    fun usaEan13CuandoElCodigoEsValido() {
        val barcode = encodeBarcode("7802820005004")!!
        assertEquals("EAN-13", barcode.format)
        assertEquals(95, barcode.modules.size)
    }

    @Test
    fun usaEan8YUpcA() {
        assertEquals("EAN-8", encodeBarcode("96385074")!!.format)
        assertEquals("UPC-A", encodeBarcode("036000291452")!!.format)
    }

    @Test
    fun usaCode128SiElDigitoVerificadorNoCoincide() {
        assertEquals("Code 128", encodeBarcode("7802820005001")!!.format)
    }

    @Test
    fun usaCode128ParaCodigosInternos() {
        val barcode = encodeBarcode("PG12345")!!
        assertEquals("Code 128", barcode.format)
        assertTrue(barcode.modules.first())
    }

    @Test
    fun codigoVacioNoGeneraBarras() {
        assertNull(encodeBarcode("   "))
    }
}
