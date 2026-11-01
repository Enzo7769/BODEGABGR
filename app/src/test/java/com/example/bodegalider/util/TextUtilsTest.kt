package com.example.bodegalider.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextUtilsTest {

    @Test
    fun quitaTildesMayusculasYEspacios() {
        assertEquals("maria gomez", "  María   Gómez ".normalizeForSearch())
        assertEquals("atun en aceite", "Atún en Aceite".normalizeForSearch())
        assertEquals("sofia", "SOFÍA".normalizeForSearch())
    }

    @Test
    fun recientesSinRepetirYConLimite() {
        val recent = listOf("Líder Maipú", "Líder Pudahuel")
        assertEquals(listOf("Líder Pudahuel", "Líder Maipú"), withRecent(recent, "  lider   pudahuel "))
        assertEquals(listOf("Líder Cerrillos", "Líder Maipú"), withRecent(recent, "Líder Cerrillos", max = 2))
        assertEquals(recent, withRecent(recent, "   "))
    }
}
