package com.example.bodegalider.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodegalider.model.Product
import com.example.bodegalider.model.TaskItem
import com.example.bodegalider.model.User
import com.example.bodegalider.ui.viewmodel.InventoryViewModel

@Composable
fun TeamScreen(viewModel: InventoryViewModel) {
    val teamUsers by viewModel.filteredTeamUsers.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val products by viewModel.products.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCommune by remember { mutableStateOf("Todas") }
    var selectedUserForDetail by remember { mutableStateOf<User?>(null) }

    val communes = listOf("Todas", "Maipú", "Pudahuel", "Cerrillos", "Stgo Centro")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Supervisión de Sector",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "Admin: ${currentUser?.name ?: "Supervisor"} | Sectores a cargo: ${currentUser?.managedCommunes?.joinToString() ?: "Ninguno"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar reponedor en tu sector...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(communes) { commune ->
                FilterChip(
                    selected = selectedCommune == commune,
                    onClick = { selectedCommune = commune },
                    label = { Text(commune) }
                )
            }
        }

        if (teamUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay reponedores en tu sector asignado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(teamUsers, key = { it.id }) { user ->
                    TeamMemberCard(user = user) {
                        selectedUserForDetail = user
                    }
                }
            }
        }
    }

    if (selectedUserForDetail != null) {
        TeamMemberDetailDialog(
            user = selectedUserForDetail!!,
            tasks = tasks.filter { it.userId == selectedUserForDetail!!.id },
            criticalProducts = products.filter { it.isOutOfStock || it.stockOnShelf == 0 || it.stockOnShelf < 5 },
            viewModel = viewModel,
            onDismiss = { selectedUserForDetail = null }
        )
    }
}

@Composable
fun TeamMemberCard(user: User, onClick: () -> Unit) {
    val statusColor = if (user.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
    val statusText = if (user.isOnline) "ONLINE" else "INACTIVO"
    val statusBg = if (user.isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Comuna: ${user.commune} • Local Código: ${user.storeCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = user.scheduleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TeamMemberDetailDialog(
    user: User,
    tasks: List<TaskItem>,
    criticalProducts: List<Product>,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var alertModalMessage by remember { mutableStateOf<String?>(null) }
    val alertVersion by viewModel.alertStateVersion.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Detalle de Reponedor y Quiebres",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Comuna: ${user.commune} | Local Asignado: ${user.storeCode}\n${user.scheduleText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                Text(
                    text = "Productos en Quiebre / Stock Bajo (Asignar Prioridad)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (criticalProducts.isEmpty()) {
                    Text(
                        text = "No hay productos críticos en este momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        criticalProducts.take(3).forEach { product ->
                            val isOnCooldown = remember(alertVersion) { viewModel.isAlertOnCooldown(user.id, product.id) }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${product.brand} - ${product.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Ubicación: ${product.locationText} | Góndola: ${product.stockOnShelf}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val (success, remainingMinutes) = viewModel.sendPriorityAlert(user.id, user.name, user.storeCode, product)
                                            alertModalMessage = if (success) {
                                                "¡Alerta enviada con éxito!\n\nSe ha notificado al reponedor ${user.name} para priorizar ${product.brand} ${product.name}.\n\nEl siguiente intento de alerta para este producto será en 1 hora más."
                                            } else {
                                                "Aviso de Cooldown:\n\nYa se envió una alerta para este producto a ${user.name} recientemente.\n\nDebes esperar $remainingMinutes minutos más para volver a alertar."
                                            }
                                        },
                                        enabled = !isOnCooldown,
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        if (!isOnCooldown) {
                                            Icon(Icons.Default.PriorityHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(if (isOnCooldown) "Ya enviado" else "Priorizar", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                Text(
                    text = "Tareas y Notificaciones Asignadas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (tasks.isEmpty()) {
                    Text(
                        text = "No hay tareas pendientes (Se eliminan automáticamente cada semana).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        tasks.forEach { task ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { viewModel.completeTask(task.id) }
                                    )
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    label = { Text("Otra nota o tarea libre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            viewModel.assignTask(user.id, user.name, user.storeCode, newTaskTitle)
                            newTaskTitle = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Task, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Nota Personalizada")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )

    if (alertModalMessage != null) {
        AlertDialog(
            onDismissRequest = { alertModalMessage = null },
            title = { Text("Estado de Alerta de Prioridad", fontWeight = FontWeight.Bold) },
            text = { Text(alertModalMessage!!) },
            confirmButton = {
                Button(onClick = { alertModalMessage = null }) {
                    Text("Entendido")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
