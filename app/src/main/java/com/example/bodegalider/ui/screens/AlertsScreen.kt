package com.example.bodegalider.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.bodegalider.model.InventoryStats
import com.example.bodegalider.model.Product
import com.example.bodegalider.model.User
import com.example.bodegalider.ui.components.EmptyState
import com.example.bodegalider.ui.components.ProductImage
import com.example.bodegalider.ui.components.ScreenHeader
import com.example.bodegalider.ui.components.StatTile
import com.example.bodegalider.ui.components.StatusBadge
import com.example.bodegalider.ui.components.UserAvatar
import com.example.bodegalider.ui.components.stockStatusPalette
import com.example.bodegalider.ui.theme.statusColors
import com.example.bodegalider.ui.viewmodel.AlertResult
import com.example.bodegalider.ui.viewmodel.InventoryViewModel
import com.example.bodegalider.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AlertsScreen(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val restockProducts by viewModel.restockProducts.collectAsState()
    val stats by viewModel.inventoryStats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentStore by viewModel.currentStore.collectAsState()
    val workers by viewModel.managedWorkers.collectAsState()

    val storeName = currentStore ?: "Local ${currentUser?.storeCode ?: "046"}"
    // El reporte PDF incluye solo lo urgente: productos en quiebre o con stock bajo
    val reportProducts = restockProducts.filter { it.isQuiebre || it.isLowStock }
    val isSupervisor = currentUser?.isSupervisor == true
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var productToAssign by remember { mutableStateOf<Product?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Quiebres y reposición",
            subtitle = if (isSupervisor) "$storeName · Sector ${currentUser?.managedCommunes?.joinToString().orEmpty()}"
            else "$storeName · productos que requieren atención"
        ) {
            FilledTonalButton(
                onClick = {
                    isGeneratingPdf = true
                    scope.launch {
                        val file = withContext(Dispatchers.IO) {
                            PdfReportGenerator.generatePdf(context, reportProducts, storeName, currentUser?.name)
                        }
                        isGeneratingPdf = false
                        if (file == null) {
                            viewModel.postMessage("No se pudo generar el PDF")
                        } else {
                            sharePdf(context, file, storeName, onError = viewModel::postMessage)
                        }
                    }
                },
                enabled = reportProducts.isNotEmpty() && !isGeneratingPdf,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (isGeneratingPdf) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reporte")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "summary") { SummaryCard(stats) }

            if (restockProducts.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Default.CheckCircle,
                        tint = MaterialTheme.statusColors.success,
                        title = "¡Todo en orden!",
                        message = "No hay productos sin stock ni pendientes de reposición."
                    )
                }
            } else {
                item(key = "title") {
                    Text(
                        text = "${restockProducts.size} productos por atender",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(restockProducts, key = { it.id }) { product ->
                    RestockProductCard(
                        product = product,
                        showAssign = isSupervisor,
                        onAssign = { productToAssign = product },
                        onClick = { viewModel.selectProduct(product) }
                    )
                }
            }
        }
    }

    productToAssign?.let { product ->
        AssignWorkerDialog(
            product = product,
            workers = workers,
            isOnCooldown = { worker -> viewModel.isAlertOnCooldown(worker.id, product.id) },
            onAssign = { worker ->
                when (val result = viewModel.sendPriorityAlert(worker, product)) {
                    AlertResult.Sent -> viewModel.postMessage("Alerta enviada a ${worker.name}")
                    is AlertResult.OnCooldown -> viewModel.postMessage(
                        "Ya se alertó a ${worker.name}. Podrás reenviar en ${result.remainingMinutes} min."
                    )
                }
                productToAssign = null
            },
            onDismiss = { productToAssign = null }
        )
    }
}

private fun sharePdf(context: Context, file: java.io.File, storeName: String, onError: (String) -> Unit) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Reporte de quiebres y stock bajo - $storeName")
        putExtra(Intent.EXTRA_TEXT, "Adjunto el reporte de quiebres y stock bajo de $storeName.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
    } catch (_: ActivityNotFoundException) {
        onError("No hay aplicaciones para compartir el PDF")
    }
}

@Composable
private fun SummaryCard(stats: InventoryStats) {
    val status = MaterialTheme.statusColors
    val availabilityColor = when {
        stats.availabilityPercent >= 90 -> status.success
        stats.availabilityPercent >= 75 -> status.warning
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Disponibilidad en góndola",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stats.availabilityPercent}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = availabilityColor
                    )
                }
                Text(
                    text = "${stats.total - stats.outOfStock} de ${stats.total} productos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { stats.availabilityPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = availabilityColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    value = "${stats.outOfStock}",
                    label = "Sin stock",
                    accent = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = "${stats.lowStock}",
                    label = "Stock bajo",
                    accent = status.warning,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = "${stats.unitsToRestock}",
                    label = "Un. a reponer",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RestockProductCard(
    product: Product,
    showAssign: Boolean,
    onAssign: () -> Unit,
    onClick: () -> Unit
) {
    val palette = stockStatusPalette(product.stockStatus)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Franja lateral con el color del estado
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(palette.accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 9.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductImage(imageUri = product.imageUri, contentDescription = product.name, size = 52.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.brand.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = product.locationText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    StatusBadge(status = product.stockStatus, modifier = Modifier.align(Alignment.Top))
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "En góndola: ${product.stockOnShelf} un.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Reponer: ${product.quantityNeeded} un.",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.accent
                    )
                }

                if (showAssign) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = onAssign, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Asignar a reponedor")
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignWorkerDialog(
    product: Product,
    workers: List<User>,
    isOnCooldown: (User) -> Boolean,
    onAssign: (User) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar reposición") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${product.displayName} · ${product.locationText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (workers.isEmpty()) {
                    Text("No hay reponedores en tu sector.")
                }
                workers.forEach { worker ->
                    val cooldown = isOnCooldown(worker)
                    ListItem(
                        headlineContent = { Text(worker.name) },
                        supportingContent = {
                            Text(
                                if (cooldown) "Alerta enviada hace menos de 1 hora"
                                else "Local ${worker.storeCode} · ${worker.commune} · ${if (worker.isOnline) "En línea" else "Inactivo"}"
                            )
                        },
                        leadingContent = { UserAvatar(user = worker, size = 40.dp) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable(enabled = !cooldown) { onAssign(worker) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
