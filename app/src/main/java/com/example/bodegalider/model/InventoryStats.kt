package com.example.bodegalider.model

import kotlin.math.roundToInt

/** Resumen del estado de la góndola, calculado a partir de la lista de productos. */
data class InventoryStats(
    val total: Int = 0,
    val outOfStock: Int = 0,
    val lowStock: Int = 0,
    val unitsToRestock: Int = 0
) {
    val healthy: Int
        get() = total - outOfStock - lowStock

    /** Porcentaje de productos con unidades en góndola (0-100). */
    val availabilityPercent: Int
        get() = if (total == 0) 100 else (((total - outOfStock) * 100f) / total).roundToInt()

    companion object {
        fun from(products: List<Product>): InventoryStats = InventoryStats(
            total = products.size,
            outOfStock = products.count { it.isQuiebre },
            lowStock = products.count { it.isLowStock },
            unitsToRestock = products.filter { it.needsRestock }.sumOf { it.quantityNeeded }
        )
    }
}
