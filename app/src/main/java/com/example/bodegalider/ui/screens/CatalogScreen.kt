package com.example.bodegalider.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bodegalider.model.Product
import com.example.bodegalider.model.StockStatus
import com.example.bodegalider.ui.components.EmptyState
import com.example.bodegalider.ui.components.ProductImage
import com.example.bodegalider.ui.components.ScreenHeader
import com.example.bodegalider.ui.components.StatTile
import com.example.bodegalider.ui.components.StatusBadge
import com.example.bodegalider.ui.components.stockStatusPalette
import com.example.bodegalider.ui.theme.statusColors
import com.example.bodegalider.ui.viewmodel.AppTab
import com.example.bodegalider.ui.viewmodel.InventoryViewModel
import com.example.bodegalider.ui.viewmodel.SortOption
import com.example.bodegalider.ui.viewmodel.StockFilter

@Composable
fun CatalogScreen(viewModel: InventoryViewModel) {
    val products by viewModel.filteredProducts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()
    val stockFilter by viewModel.stockFilter.collectAsState()
    val currentStore by viewModel.currentStore.collectAsState()
    val visibleTasks by viewModel.visibleTasks.collectAsState()
    val stats by viewModel.inventoryStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val pendingTasks = visibleTasks.filter { !it.isCompleted }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Catálogo de góndola",
            subtitle = currentStore
        ) {
            SortMenu(current = currentSort, onSelect = viewModel::setSortOption)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (pendingTasks.isNotEmpty()) {
                item(key = "tasks") {
                    PendingTasksBanner(
                        count = pendingTasks.size,
                        preview = pendingTasks.first().title,
                        onClick = { viewModel.setTab(AppTab.NOTIFICATIONS) }
                    )
                }
            }

            item(key = "search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar producto, marca, pasillo o código") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
            }

            item(key = "filters") {
                val status = MaterialTheme.statusColors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        value = "${stats.total}",
                        label = StockFilter.ALL.label,
                        accent = MaterialTheme.colorScheme.primary,
                        selected = stockFilter == StockFilter.ALL,
                        onClick = { viewModel.setStockFilter(StockFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        value = "${stats.outOfStock}",
                        label = StockFilter.OUT_OF_STOCK.label,
                        accent = MaterialTheme.colorScheme.error,
                        selected = stockFilter == StockFilter.OUT_OF_STOCK,
                        onClick = { viewModel.setStockFilter(StockFilter.OUT_OF_STOCK) },
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        value = "${stats.lowStock}",
                        label = StockFilter.LOW.label,
                        accent = status.warning,
                        selected = stockFilter == StockFilter.LOW,
                        onClick = { viewModel.setStockFilter(StockFilter.LOW) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            when {
                isLoading && products.isEmpty() -> item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                products.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "Sin resultados",
                        message = if (searchQuery.isNotBlank() || stockFilter != StockFilter.ALL)
                            "No hay productos que coincidan con la búsqueda o el filtro."
                        else
                            "Aún no hay productos registrados. Escanea un código o toca \"Nuevo producto\".",
                        actionLabel = if (searchQuery.isNotBlank() || stockFilter != StockFilter.ALL) "Limpiar filtros" else null,
                        onAction = {
                            viewModel.setSearchQuery("")
                            viewModel.setStockFilter(StockFilter.ALL)
                        }
                    )
                }

                else -> items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onIncrement = { viewModel.adjustStock(product.id, 1) },
                        onDecrement = { viewModel.adjustStock(product.id, -1) },
                        onClick = { viewModel.selectProduct(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortMenu(current: SortOption, onSelect: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = "Ordenar por",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (option == current) Icon(Icons.Default.Check, contentDescription = "Seleccionado")
                    }
                )
            }
        }
    }
}

@Composable
private fun PendingTasksBanner(count: Int, preview: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (count == 1) "Tienes 1 tarea pendiente" else "Tienes $count tareas pendientes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ver avisos",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onClick: () -> Unit
) {
    val palette = stockStatusPalette(product.stockStatus)
    val containerColor = if (product.stockStatus == StockStatus.OK) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        palette.container.copy(alpha = 0.35f).compositeOver(MaterialTheme.colorScheme.surface)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductImage(imageUri = product.imageUri, contentDescription = product.name)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.brand.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "En góndola",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (product.quantityNeeded > 0) "Faltan ${product.quantityNeeded} un. por reponer" else "Góndola completa",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.quantityNeeded > 0) palette.accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StockStepper(
                    value = product.stockOnShelf,
                    onDecrement = onDecrement,
                    onIncrement = onIncrement
                )
            }
        }
    }
}

/** Control de - / cantidad / + con áreas táctiles amplias para uso con guantes o en movimiento. */
@Composable
fun StockStepper(value: Int, onDecrement: () -> Unit, onIncrement: () -> Unit, large: Boolean = false) {
    val buttonSize = if (large) 56.dp else 44.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = onDecrement,
            enabled = value > 0,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Restar una unidad")
        }
        Text(
            text = "$value",
            modifier = Modifier.widthIn(min = if (large) 72.dp else 48.dp),
            style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        FilledIconButton(
            onClick = onIncrement,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Sumar una unidad")
        }
    }
}
