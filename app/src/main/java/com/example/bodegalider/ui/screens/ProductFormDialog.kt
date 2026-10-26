package com.example.bodegalider.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.bodegalider.model.Product
import com.example.bodegalider.ui.components.ProductImage
import com.example.bodegalider.ui.components.ScannableBarcode
import com.example.bodegalider.ui.components.SectionTitle

/**
 * Formulario compartido para registrar y editar productos.
 * [isBarcodeTaken] permite validar que el código no esté asignado a otro producto.
 */
@Composable
fun ProductFormDialog(
    title: String,
    confirmLabel: String,
    initial: Product,
    barcodeEditable: Boolean,
    isBarcodeTaken: (String) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    var barcode by remember { mutableStateOf(initial.barcode) }
    var name by remember { mutableStateOf(initial.name) }
    var brand by remember { mutableStateOf(initial.brand) }
    var stock by remember { mutableStateOf(initial.stockOnShelf.toString()) }
    var needed by remember { mutableStateOf(initial.quantityNeeded.toString()) }
    var aisle by remember { mutableStateOf(initial.aisle) }
    var locationDetail by remember { mutableStateOf(initial.locationDetail) }
    var imageUri by remember { mutableStateOf(initial.imageUri) }
    var isQuiebre by remember { mutableStateOf(initial.isOutOfStock && initial.stockOnShelf > 0) }
    var showErrors by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Permiso persistente para seguir mostrando la foto después de reiniciar la app
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
            imageUri = uri.toString()
        }
    }

    val barcodeError = when {
        barcode.isBlank() -> "Ingresa el código de barras"
        barcodeEditable && isBarcodeTaken(barcode) -> "Este código ya está registrado"
        else -> null
    }
    val nameError = if (name.isBlank()) "Ingresa el nombre" else null
    val brandError = if (brand.isBlank()) "Ingresa la marca" else null
    val isValid = barcodeError == null && nameError == null && brandError == null
    val stockValue = stock.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Al abrir un producto existente se muestra su código listo para pistolear
                if (!barcodeEditable) {
                    ScannableBarcode(code = initial.barcode)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductImage(imageUri = imageUri, contentDescription = "Foto del producto", size = 72.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { photoLauncher.launch(arrayOf("image/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (imageUri.isBlank()) "Agregar foto" else "Cambiar foto")
                        }
                        if (imageUri.isNotBlank()) {
                            TextButton(onClick = { imageUri = "" }) { Text("Quitar foto") }
                        }
                    }
                }

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it.filter(Char::isLetterOrDigit).take(32) },
                    label = { Text("Código de barras") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    enabled = barcodeEditable,
                    isError = showErrors && barcodeError != null,
                    supportingText = if (showErrors && barcodeError != null) ({ Text(barcodeError) }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marca") },
                    isError = showErrors && brandError != null,
                    supportingText = if (showErrors && brandError != null) ({ Text(brandError) }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre y formato (ej: Leche Entera 1L)") },
                    isError = showErrors && nameError != null,
                    supportingText = if (showErrors && nameError != null) ({ Text(nameError) }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                SectionTitle("Ubicación en sala")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = aisle,
                        onValueChange = { aisle = it },
                        label = { Text("Pasillo") },
                        placeholder = { Text("Pasillo 3") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = locationDetail,
                        onValueChange = { locationDetail = it },
                        label = { Text("Detalle") },
                        placeholder = { Text("Cartel 12") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }

                SectionTitle("Stock")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it.filter(Char::isDigit).take(5) },
                        label = { Text("En góndola") },
                        suffix = { Text("un.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = needed,
                        onValueChange = { needed = it.filter(Char::isDigit).take(5) },
                        label = { Text("Por reponer") },
                        suffix = { Text("un.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Quiebre confirmado", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (stockValue == 0) "Sin unidades en góndola: se marca como quiebre automáticamente."
                            else "No hay en bodega y no llegará reposición pronto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isQuiebre || stockValue == 0,
                        onCheckedChange = { isQuiebre = it },
                        enabled = stockValue > 0
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isValid) {
                        showErrors = true
                        return@Button
                    }
                    onSave(
                        initial.copy(
                            barcode = barcode.trim(),
                            name = name.trim(),
                            brand = brand.trim(),
                            imageUri = imageUri,
                            stockOnShelf = stockValue,
                            quantityNeeded = needed.toIntOrNull() ?: 0,
                            isOutOfStock = isQuiebre || stockValue == 0,
                            aisle = aisle.trim(),
                            locationDetail = locationDetail.trim()
                        )
                    )
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
