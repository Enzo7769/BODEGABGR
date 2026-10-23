package com.example.bodegalider.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodegalider.model.UserRole
import com.example.bodegalider.ui.viewmodel.AppTab
import com.example.bodegalider.ui.viewmodel.InventoryViewModel

@Composable
fun MainScreen(viewModel: InventoryViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val isAddDialogOpen by viewModel.isAddProductDialogOpen.collectAsState()
    val scannedBarcode by viewModel.scannedBarcodeForNew.collectAsState()
    val products by viewModel.products.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    val isAdmin = currentUser?.role == UserRole.ADMIN
    val quiebreCount = products.count { it.isOutOfStock || it.stockOnShelf == 0 }
    val pendingTasksCount = tasks.count { 
        if (isAdmin) !it.isCompleted else (currentUser != null && it.userId == currentUser!!.id && !it.isCompleted) 
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                if (isAdmin) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.TEAM,
                        onClick = { viewModel.setTab(AppTab.TEAM) },
                        icon = { Icon(Icons.Default.Group, contentDescription = "Equipo") },
                        label = { Text("Equipo") }
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.ALERTS,
                        onClick = { viewModel.setTab(AppTab.ALERTS) },
                        icon = {
                            if (quiebreCount > 0) {
                                BadgedBox(badge = { Badge { Text("$quiebreCount") } }) {
                                    Icon(Icons.Default.Warning, contentDescription = "Quiebres")
                                }
                            } else {
                                Icon(Icons.Default.Warning, contentDescription = "Quiebres")
                            }
                        },
                        label = { Text("Quiebres") }
                    )
                } else {
                    NavigationBarItem(
                        selected = currentTab == AppTab.CATALOG,
                        onClick = { viewModel.setTab(AppTab.CATALOG) },
                        icon = { Icon(Icons.Default.Inventory, contentDescription = "Catálogo") },
                        label = { Text("Catálogo") }
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.ALERTS,
                        onClick = { viewModel.setTab(AppTab.ALERTS) },
                        icon = {
                            if (quiebreCount > 0) {
                                BadgedBox(badge = { Badge { Text("$quiebreCount") } }) {
                                    Icon(Icons.Default.Warning, contentDescription = "Quiebres")
                                }
                            } else {
                                Icon(Icons.Default.Warning, contentDescription = "Quiebres")
                            }
                        },
                        label = { Text("Quiebres") }
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.SCANNER,
                        onClick = { viewModel.setTab(AppTab.SCANNER) },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear") },
                        label = { Text("Escanear") }
                    )
                }

                // Pestaña de Notificaciones (Común para Admin y Usuario)
                NavigationBarItem(
                    selected = currentTab == AppTab.NOTIFICATIONS,
                    onClick = { viewModel.setTab(AppTab.NOTIFICATIONS) },
                    icon = {
                        if (pendingTasksCount > 0) {
                            BadgedBox(badge = { Badge { Text("$pendingTasksCount") } }) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "Notificaciones")
                            }
                        } else {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Notificaciones")
                        }
                    },
                    label = { Text("Avisos") }
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { viewModel.setTab(AppTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.CATALOG -> if (!isAdmin) CatalogScreen(viewModel = viewModel) { viewModel.setTab(AppTab.SCANNER) }
                AppTab.ALERTS -> AlertsScreen(viewModel = viewModel)
                AppTab.SCANNER -> if (!isAdmin) ScannerScreen(viewModel = viewModel) { }
                AppTab.TEAM -> TeamScreen(viewModel = viewModel)
                AppTab.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }

        if (selectedProduct != null) {
            EditProductDialog(
                product = selectedProduct!!,
                viewModel = viewModel,
                onDismiss = { viewModel.selectProduct(null) }
            )
        }

        if (isAddDialogOpen) {
            AddProductDialog(
                initialBarcode = scannedBarcode,
                viewModel = viewModel,
                onDismiss = { viewModel.closeAddProductDialog() }
            )
        }
    }
}
