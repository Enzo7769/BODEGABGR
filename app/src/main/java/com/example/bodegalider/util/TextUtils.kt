package com.example.bodegalider.util

import java.text.Normalizer

/** Pasa a minúsculas y quita tildes/espacios extra: "  María " -> "maria". */
fun String.normalizeForSearch(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("\\s+"), " ")

/**
 * Pone [item] primero en la lista de usados recientemente, sin repetirlo
 * (ignorando mayúsculas y tildes) y conservando como máximo [max] elementos.
 */
fun withRecent(recent: List<String>, item: String, max: Int = 8): List<String> {
    val clean = item.trim().replace(Regex("\\s+"), " ")
    if (clean.isEmpty()) return recent
    val key = clean.normalizeForSearch()
    // Si ya estaba, conserva cómo se escribió la primera vez ("Líder Maipú" y no "lider maipu")
    val existing = recent.firstOrNull { it.normalizeForSearch() == key } ?: clean
    return (listOf(existing) + recent.filter { it.normalizeForSearch() != key }).take(max)
}
