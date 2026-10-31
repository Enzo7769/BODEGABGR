package com.example.bodegalider.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.bodegalider.model.Product
import com.example.bodegalider.ui.viewmodel.InventoryViewModel

@Composable
fun AddProductDialog(
    initialBarcode: String,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val initial = remember(initialBarcode) {
        Product(id = "", barcode = initialBarcode, name = "", brand = "", quantityNeeded = 10)
    }
    val brands by viewModel.knownBrands.collectAsState()
    ProductFormDialog(
        title = "Registrar producto",
        confirmLabel = "Registrar",
        initial = initial,
        // Si viene del escáner el código ya es correcto; si se crea a mano se puede escribir
        barcodeEditable = initialBarcode.isBlank(),
        brands = brands,
        isBarcodeTaken = { viewModel.isBarcodeTaken(it) },
        onDismiss = onDismiss,
        onSave = { viewModel.addNewProduct(it) }
    )
}
