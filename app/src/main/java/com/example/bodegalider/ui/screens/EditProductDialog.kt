package com.example.bodegalider.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.bodegalider.model.Product
import com.example.bodegalider.ui.viewmodel.InventoryViewModel

@Composable
fun EditProductDialog(
    product: Product,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    var stockInput by remember { mutableStateOf(product.stockOnShelf.toString()) }
    var neededInput by remember { mutableStateOf(product.quantityNeeded.toString()) }
    var aisleInput by remember { mutableStateOf(product.aisle) }
    var locationDetailInput by remember { mutableStateOf(product.locationDetail) }
    var isQuiebreInput by remember { mutableStateOf(product.isOutOfStock) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = product.brand.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Código de barras: ${product.barcode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Campos de ubicación
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = aisleInput,
                        onValueChange = { aisleInput = it },
                        label = { Text("Pasillo (ej: Pasillo 3)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = locationDetailInput,
                        onValueChange = { locationDetailInput = it },
                        label = { Text("Ubicación (Cartel/Cabecera/Isla)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = stockInput,
                    onValueChange = { stockInput = it },
                    label = { Text("Cantidad en Góndola") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = neededInput,
                    onValueChange = { neededInput = it },
                    label = { Text("Por rellenar / Faltantes (unidades)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isQuiebreInput,
                        onCheckedChange = { isQuiebreInput = it }
                    )
                    Text(
                        text = "Marcar en quiebra (Sin productos en góndola ni bodega y tampoco llegarán)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newStock = stockInput.toIntOrNull() ?: product.stockOnShelf
                    val newNeeded = neededInput.toIntOrNull() ?: product.quantityNeeded
                    viewModel.updateStock(
                        productId = product.id,
                        stock = newStock,
                        needed = newNeeded,
                        isQuiebre = isQuiebreInput,
                        aisle = aisleInput,
                        locationDetail = locationDetailInput
                    )
                    onDismiss()
                }
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
