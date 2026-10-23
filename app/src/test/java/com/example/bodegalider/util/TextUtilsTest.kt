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
}
