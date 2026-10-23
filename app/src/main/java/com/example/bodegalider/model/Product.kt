package com.example.bodegalider.model

data class Product(
    val id: String,
    val barcode: String,
    val name: String,
    val brand: String,
    val imageResId: Int = 0, // Recurso de imagen local por defecto
    val imageUri: String = "", // URI de foto seleccionada de la galería/cámara
    val stockOnShelf: Int = 0, // Cantidad actual en góndola
    val quantityNeeded: Int = 0, // Cantidad faltante por rellenar
    val isOutOfStock: Boolean = false, // Estado explícito de quiebre
    val aisle: String = "", // Número o nombre de pasillo (ej: "Pasillo 4")
    val locationDetail: String = "" // Detalle (ej: "Cartel 12", "Cabecera", "Isla")
) {
    val statusText: String
        get() = when {
            isOutOfStock || stockOnShelf == 0 -> "Sin productos en góndola"
            stockOnShelf < 5 -> "Stock Bajo"
            else -> "En Góndola"
        }

    val locationText: String
        get() = when {
            aisle.isNotBlank() && locationDetail.isNotBlank() -> "$aisle • $locationDetail"
            aisle.isNotBlank() -> aisle
            locationDetail.isNotBlank() -> locationDetail
            else -> "Ubicación no asignada"
        }
}
