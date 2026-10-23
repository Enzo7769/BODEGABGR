package com.example.bodegalider.model

enum class StockStatus(val label: String) {
    OUT_OF_STOCK("Sin stock"),
    LOW("Stock bajo"),
    OK("En góndola")
}

data class Product(
    val id: String,
    val barcode: String,
    val name: String,
    val brand: String,
    val imageUri: String = "", // URI de la foto elegida desde la galería
    val stockOnShelf: Int = 0, // Cantidad actual en góndola
    val quantityNeeded: Int = 0, // Cantidad faltante por rellenar
    val isOutOfStock: Boolean = false, // Quiebre confirmado (no hay en bodega ni viene en camino)
    val aisle: String = "", // Número o nombre de pasillo (ej: "Pasillo 4")
    val locationDetail: String = "" // Detalle (ej: "Cartel 12", "Cabecera", "Isla")
) {
    val isQuiebre: Boolean
        get() = isOutOfStock || stockOnShelf <= 0

    val isLowStock: Boolean
        get() = !isQuiebre && stockOnShelf < LOW_STOCK_THRESHOLD

    /** Producto que debe aparecer en la lista de quiebres / reposición. */
    val needsRestock: Boolean
        get() = isQuiebre || isLowStock || quantityNeeded > RESTOCK_ALERT_THRESHOLD

    val stockStatus: StockStatus
        get() = when {
            isQuiebre -> StockStatus.OUT_OF_STOCK
            isLowStock -> StockStatus.LOW
            else -> StockStatus.OK
        }

    val statusText: String
        get() = stockStatus.label

    val displayName: String
        get() = if (brand.isBlank()) name else "$brand $name"

    val locationText: String
        get() = when {
            aisle.isNotBlank() && locationDetail.isNotBlank() -> "$aisle • $locationDetail"
            aisle.isNotBlank() -> aisle
            locationDetail.isNotBlank() -> locationDetail
            else -> "Ubicación no asignada"
        }

    companion object {
        const val LOW_STOCK_THRESHOLD = 5
        const val RESTOCK_ALERT_THRESHOLD = 5
    }
}
