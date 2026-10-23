package com.example.bodegalider.model

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryStatsTest {

    private fun product(id: String, stock: Int, needed: Int) =
        Product(id = id, barcode = id, name = "P$id", brand = "B", stockOnShelf = stock, quantityNeeded = needed)

    @Test
    fun calculaResumen() {
        val stats = InventoryStats.from(
            listOf(
                product("1", stock = 0, needed = 10), // quiebre
                product("2", stock = 2, needed = 8), // stock bajo
                product("3", stock = 20, needed = 1), // ok, no cuenta para reponer
                product("4", stock = 20, needed = 7) // ok, pero con faltante alto
            )
        )
        assertEquals(4, stats.total)
        assertEquals(1, stats.outOfStock)
        assertEquals(1, stats.lowStock)
        assertEquals(2, stats.healthy)
        assertEquals(25, stats.unitsToRestock)
        assertEquals(75, stats.availabilityPercent)
    }

    @Test
    fun listaVaciaTieneDisponibilidadCompleta() {
        assertEquals(100, InventoryStats.from(emptyList()).availabilityPercent)
    }
}
