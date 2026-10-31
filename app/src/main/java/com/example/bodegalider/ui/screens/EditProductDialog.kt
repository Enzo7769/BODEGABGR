package com.example.bodegalider.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.bodegalider.model.Product
import com.example.bodegalider.ui.viewmodel.InventoryViewModel

@Composable
fun EditProductDialog(
    product: Product,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val brands by viewModel.knownBrands.collectAsState()
    ProductFormDialog(
        title = "Editar producto",
        confirmLabel = "Guardar cambios",
        initial = product,
        barcodeEditable = false,
        brands = brands,
        isBarcodeTaken = { viewModel.isBarcodeTaken(it, exceptProductId = product.id) },
        onDismiss = onDismiss,
        onSave = {
            viewModel.updateProduct(it)
            onDismiss()
        }
    )
}
