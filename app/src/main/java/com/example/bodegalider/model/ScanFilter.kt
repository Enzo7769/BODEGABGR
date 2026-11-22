package com.example.bodegalider.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/** Periodos para filtrar productos por la fecha y hora de su último escaneo. */
enum class ScanPeriod(val label: String) {
    ALL("Todos"),
    TODAY("Hoy"),
    LAST_24H("Últimas 24 h"),
    LAST_7_DAYS("Últimos 7 días"),
    CUSTOM("Rango")
}

/** Rango de tiempo cerrado [fromMillis, toMillis]. */
data class ScanRange(val fromMillis: Long, val toMillis: Long) {
    operator fun contains(millis: Long): Boolean = millis in fromMillis..toMillis
}

/** Rango que corresponde al periodo; null significa sin filtro. CUSTOM usa [custom]. */
fun ScanPeriod.range(now: Long, zone: ZoneId, custom: ScanRange? = null): ScanRange? = when (this) {
    ScanPeriod.ALL -> null
    ScanPeriod.TODAY -> ScanRange(LocalDate.now(zone.clockAt(now)).atStartOfDay(zone).toInstant().toEpochMilli(), now)
    ScanPeriod.LAST_24H -> ScanRange(now - TimeUnit.HOURS.toMillis(24), now)
    ScanPeriod.LAST_7_DAYS -> ScanRange(now - TimeUnit.DAYS.toMillis(7), now)
    ScanPeriod.CUSTOM -> custom
}

/** Productos escaneados dentro del rango; sin rango devuelve la lista completa. */
fun List<Product>.scannedWithin(range: ScanRange?): List<Product> =
    if (range == null) this else filter { product -> product.lastScannedAt?.let { it in range } == true }

/** "Hoy 14:35", "Ayer 09:10" o "21/11/2026 14:35". */
fun formatScanTime(millis: Long, now: Long, zone: ZoneId): String {
    val time = Instant.ofEpochMilli(millis).atZone(zone)
    val today = LocalDate.now(zone.clockAt(now))
    val hour = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (time.toLocalDate()) {
        today -> "Hoy $hour"
        today.minusDays(1) -> "Ayer $hour"
        else -> time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }
}

/**
 * Junta la fecha elegida en un calendario (medianoche UTC, como la entrega el DatePicker)
 * con una hora local, y devuelve el instante en milisegundos.
 */
fun combineDateAndTime(utcDateMillis: Long, hour: Int, minute: Int, zone: ZoneId): Long {
    val date = Instant.ofEpochMilli(utcDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
}

/** Día del instante como medianoche UTC, el formato que usa el DatePicker para preseleccionarlo. */
fun utcDateMillis(millis: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Hora y minuto locales del instante. */
fun localHourMinute(millis: Long, zone: ZoneId): Pair<Int, Int> =
    Instant.ofEpochMilli(millis).atZone(zone).let { it.hour to it.minute }

fun formatDate(millis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun formatTime(millis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))

private fun ZoneId.clockAt(now: Long) = java.time.Clock.fixed(Instant.ofEpochMilli(now), this)
