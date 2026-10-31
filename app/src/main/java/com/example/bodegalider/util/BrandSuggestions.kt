package com.example.bodegalider.util

/** Marcas sin repetir (ignorando mayúsculas y tildes), ordenadas alfabéticamente. */
fun distinctBrands(brands: Iterable<String>): List<String> =
    brands.map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy(String::normalizeForSearch)
        .sortedBy(String::normalizeForSearch)

/**
 * Marcas que coinciden con lo escrito, para elegir sin tipear todo.
 * Con el campo vacío muestra todas; si lo escrito ya es una marca exacta no sugiere nada.
 */
fun brandSuggestions(brands: List<String>, query: String): List<String> {
    val needle = query.normalizeForSearch()
    if (needle.isEmpty()) return brands
    if (brands.any { it.normalizeForSearch() == needle }) return emptyList()
    return brands.filter { needle in it.normalizeForSearch() }
}

/** Si lo escrito es una marca conocida con otra escritura ("pantene"), usa la escritura registrada ("Pantene"). */
fun canonicalBrand(brands: List<String>, typed: String): String {
    val needle = typed.normalizeForSearch()
    return brands.firstOrNull { it.normalizeForSearch() == needle } ?: typed.trim()
}
