package com.example.bodegalider.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductTest {

    private fun product(stock: Int, needed: Int = 0, outOfStock: Boolean = false, aisle: String = "", detail: String = "") =
        Product(
            id = "1", barcode = "123", name = "Leche", brand = "Colun",
            stockOnShelf = stock, quantityNeeded = needed, isOutOfStock = outOfStock,
            aisle = aisle, locationDetail = detail
        )

    @Test
    fun sinUnidadesEsQuiebre() {
        val p = product(stock = 0)
        assertTrue(p.isQuiebre)
        assertFalse(p.isLowStock)
        assertEquals(StockStatus.OUT_OF_STOCK, p.stockStatus)
    }

    @Test
    fun quiebreConfirmadoAunqueHayaUnidades() {
        assertEquals(StockStatus.OUT_OF_STOCK, product(stock = 3, outOfStock = true).stockStatus)
    }

    @Test
    fun stockBajoBajoElUmbral() {
        val p = product(stock = Product.LOW_STOCK_THRESHOLD - 1)
        assertTrue(p.isLowStock)
        assertEquals(StockStatus.LOW, p.stockStatus)
        assertTrue(p.needsRestock)
    }

    @Test
    fun stockSuficienteNoRequiereReposicionSalvoFaltantesAltos() {
        assertFalse(product(stock = 12, needed = 2).needsRestock)
        assertTrue(product(stock = 12, needed = Product.RESTOCK_ALERT_THRESHOLD + 1).needsRestock)
        assertEquals(StockStatus.OK, product(stock = 12).stockStatus)
    }

    @Test
    fun textoDeUbicacion() {
        assertEquals("Pasillo 2 • Cartel 5", product(1, aisle = "Pasillo 2", detail = "Cartel 5").locationText)
        assertEquals("Pasillo 2", product(1, aisle = "Pasillo 2").locationText)
        assertEquals("Cabecera", product(1, detail = "Cabecera").locationText)
        assertEquals("Ubicación no asignada", product(1).locationText)
    }
}
