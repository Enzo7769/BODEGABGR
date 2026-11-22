package com.example.bodegalider.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ScanFilterTest {

    private val zone = ZoneId.of("America/Santiago")
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()
    private val now = at("2026-11-22T15:00")

    private fun product(id: String, scannedAt: Long?) =
        Product(id = id, barcode = id, name = id, brand = "", lastScannedAt = scannedAt)

    private val products = listOf(
        product("hoy", at("2026-11-22T08:30")),
        product("ayer", at("2026-11-21T20:00")),
        product("semana", at("2026-11-17T10:00")),
        product("viejo", at("2026-10-01T10:00")),
        product("nunca", null)
    )

    @Test
    fun todosNoFiltra() {
        assertNull(ScanPeriod.ALL.range(now, zone))
        assertEquals(products, products.scannedWithin(null))
    }

    @Test
    fun hoyEmpiezaALaMedianocheLocal() {
        val range = ScanPeriod.TODAY.range(now, zone)
        assertEquals(listOf("hoy"), products.scannedWithin(range).map { it.id })
    }

    @Test
    fun ultimas24hYUltimos7Dias() {
        assertEquals(listOf("hoy", "ayer"), products.scannedWithin(ScanPeriod.LAST_24H.range(now, zone)).map { it.id })
        assertEquals(
            listOf("hoy", "ayer", "semana"),
            products.scannedWithin(ScanPeriod.LAST_7_DAYS.range(now, zone)).map { it.id }
        )
    }

    @Test
    fun rangoPersonalizadoIncluyeLosBordes() {
        val custom = ScanRange(at("2026-11-21T20:00"), at("2026-11-22T08:30"))
        assertEquals(listOf("hoy", "ayer"), products.scannedWithin(ScanPeriod.CUSTOM.range(now, zone, custom)).map { it.id })
    }

    @Test
    fun formateaHoyAyerYFecha() {
        assertEquals("Hoy 08:30", formatScanTime(at("2026-11-22T08:30"), now, zone))
        assertEquals("Ayer 20:00", formatScanTime(at("2026-11-21T20:00"), now, zone))
        assertEquals("17/11/2026 10:00", formatScanTime(at("2026-11-17T10:00"), now, zone))
    }

    @Test
    fun combinaFechaDelCalendarioConHoraLocal() {
        val pickerDate = LocalDateTime.parse("2026-11-21T00:00").toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(at("2026-11-21T18:45"), combineDateAndTime(pickerDate, 18, 45, zone))
    }

    @Test
    fun idaYVueltaConElCalendario() {
        val instant = at("2026-11-21T23:30")
        val day = utcDateMillis(instant, zone)
        val (hour, minute) = localHourMinute(instant, zone)
        assertEquals(instant, combineDateAndTime(day, hour, minute, zone))
        assertEquals("21/11/2026", formatDate(instant, zone))
        assertEquals("23:30", formatTime(instant, zone))
    }
}
