package com.example.bodegalider.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BrandSuggestionsTest {

    private val brands = distinctBrands(listOf("Pantene", "Tío", "Ariel", "pantene ", "", "Head & Shoulders", "TIO"))

    @Test
    fun quitaRepetidosYOrdena() {
        assertEquals(listOf("Ariel", "Head & Shoulders", "Pantene", "Tío"), brands)
    }

    @Test
    fun campoVacioMuestraTodas() {
        assertEquals(brands, brandSuggestions(brands, "  "))
    }

    @Test
    fun filtraSinImportarTildesNiMayusculas() {
        assertEquals(listOf("Pantene"), brandSuggestions(brands, "PAN"))
        assertEquals(listOf("Head & Shoulders"), brandSuggestions(brands, "shoul"))
        assertEquals(listOf("Tío"), brandSuggestions(brands, "ti"))
    }

    @Test
    fun marcaExactaYaNoSugiere() {
        assertEquals(emptyList<String>(), brandSuggestions(brands, "tio"))
    }

    @Test
    fun usaLaEscrituraRegistrada() {
        assertEquals("Pantene", canonicalBrand(brands, " pantene"))
        assertEquals("Tío", canonicalBrand(brands, "TIO"))
        assertEquals("Marca Nueva", canonicalBrand(brands, " Marca Nueva "))
    }
}
