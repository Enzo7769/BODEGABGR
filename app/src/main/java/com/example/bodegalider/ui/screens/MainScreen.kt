package com.example.bodegalider.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.bodegalider.ui.viewmodel.AppTab
import com.example.bodegalider.ui.viewmodel.InventoryViewModel

private data class NavDestination(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun MainScreen(viewModel: InventoryViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val isAddDialogOpen by viewModel.isAddProductDialogOpen.collectAsState()
    val scannedBarcode by viewModel.scannedBarcodeForNew.collectAsState()
    val stats by viewModel.inventoryStats.collectAsState()
    val visibleTasks by viewModel.visibleTasks.collectAsState()

    val isSupervisor = currentUser?.isSupervisor == true
    val pendingTasksCount = visibleTasks.count { !it.isCompleted }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val destinations = buildList {
        if (isSupervisor) {
            add(NavDestination(AppTab.TEAM, "Equipo", Icons.Outlined.Groups, Icons.Filled.Groups))
        } else {
            add(NavDestination(AppTab.CATALOG, "Catálogo", Icons.Outlined.Inventory2, Icons.Filled.Inventory2))
        }
        add(NavDestination(AppTab.ALERTS, "Quiebres", Icons.Outlined.WarningAmber, Icons.Filled.Warning, stats.outOfStock))
        if (!isSupervisor) {
            add(NavDestination(AppTab.SCANNER, "Escanear", Icons.Outlined.QrCodeScanner, Icons.Filled.QrCodeScanner))
        }
        add(NavDestination(AppTab.NOTIFICATIONS, "Avisos", Icons.Outlined.Notifications, Icons.Filled.Notifications, pendingTasksCount))
        add(NavDestination(AppTab.SETTINGS, "Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings))
    }

    // Si la pestaña actual no corresponde al rol (p. ej. tras cambiar de usuario) se vuelve a la primera
    val activeTab = if (destinations.any { it.tab == currentTab }) currentTab else destinations.first().tab

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (activeTab == AppTab.CATALOG) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddProduct() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo producto") }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = activeTab == destination.tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.setTab(destination.tab) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (destination.badgeCount > 0) {
                                        Badge { Text(if (destination.badgeCount > 99) "99+" else "${destination.badgeCount}") }
                                    }
                                }
                            ) {
                                Icon(
                                    if (selected) destination.selectedIcon else destination.icon,
                                    contentDescription = destination.label
                                )
                            }
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (activeTab) {
                AppTab.CATALOG -> CatalogScreen(viewModel = viewModel)
                AppTab.ALERTS -> AlertsScreen(viewModel = viewModel)
                AppTab.SCANNER -> ScannerScreen(viewModel = viewModel)
                AppTab.TEAM -> TeamScreen(viewModel = viewModel)
                AppTab.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }

        selectedProduct?.let { product ->
            EditProductDialog(
                product = product,
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
