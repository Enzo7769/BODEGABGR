package com.example.bodegalider.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bodegalider.ui.components.Pill
import com.example.bodegalider.ui.components.ScreenHeader
import com.example.bodegalider.ui.components.UserAvatar
import com.example.bodegalider.ui.theme.statusColors
import com.example.bodegalider.ui.viewmodel.InventoryViewModel

@Composable
fun SettingsScreen(viewModel: InventoryViewModel) {
    val isOnline by viewModel.isOnline.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    }
    val status = MaterialTheme.statusColors
    val listColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = "Ajustes")

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Perfil
            currentUser?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(user = user, size = 56.dp, showStatus = false)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = user.role.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(4.dp))
                        ListItem(
                            headlineContent = { Text("Local ${user.storeCode} · ${user.commune}") },
                            supportingContent = if (user.managedCommunes.isNotEmpty()) {
                                ({ Text("A cargo de: ${user.managedCommunes.joinToString()}") })
                            } else {
                                null
                            },
                            leadingContent = { Icon(Icons.Default.Storefront, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        )
                        ListItem(
                            headlineContent = { Text(user.scheduleText) },
                            leadingContent = { Icon(Icons.Default.Schedule, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                }
            }

            // Sincronización
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isOnline) status.success else status.warning
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sincronización", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Pill(
                            text = if (isOnline) "En línea" else "Sin conexión",
                            container = if (isOnline) status.successContainer else status.warningContainer,
                            content = if (isOnline) status.onSuccessContainer else status.onWarningContainer
                        )
                    }
                    Text(
                        text = if (isOnline) "Los cambios de stock y los escaneos se guardan en la nube y los ve todo el equipo."
                        else "Puedes seguir trabajando: los cambios quedan guardados en este equipo y se enviarán al recuperar la señal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { viewModel.refreshNetworkStatus() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Comprobar conexión")
                    }
                }
            }

            // Apariencia y acerca de
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                ListItem(
                    headlineContent = { Text("Modo oscuro") },
                    supportingContent = { Text(if (isDarkMode) "Activado" else "Desactivado") },
                    leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                    trailingContent = {
                        Switch(checked = isDarkMode, onCheckedChange = viewModel::toggleDarkMode)
                    },
                    colors = listColors
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Versión de la app") },
                    supportingContent = { Text("Bodega Líder $versionName") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    colors = listColors
                )
            }

            Button(
                onClick = { confirmLogout = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que volver a ingresar tu usuario para usar la app.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmLogout = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") }
            }
        )
    }
}
