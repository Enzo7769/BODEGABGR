package com.example.bodegalider.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bodegalider.model.Product
import com.example.bodegalider.model.TaskItem
import com.example.bodegalider.model.User
import com.example.bodegalider.ui.components.EmptyState
import com.example.bodegalider.ui.components.Pill
import com.example.bodegalider.ui.components.ScreenHeader
import com.example.bodegalider.ui.components.SectionTitle
import com.example.bodegalider.ui.components.StatTile
import com.example.bodegalider.ui.components.StatusBadge
import com.example.bodegalider.ui.components.UserAvatar
import com.example.bodegalider.ui.theme.statusColors
import com.example.bodegalider.ui.viewmodel.AlertResult
import com.example.bodegalider.ui.viewmodel.InventoryViewModel

@Composable
fun TeamScreen(viewModel: InventoryViewModel) {
    val teamUsers by viewModel.filteredTeamUsers.collectAsState()
    val allWorkers by viewModel.managedWorkers.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val restockProducts by viewModel.restockProducts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val searchQuery by viewModel.teamSearchQuery.collectAsState()
    val selectedCommune by viewModel.selectedCommuneFilter.collectAsState()
    val communes by viewModel.availableCommunes.collectAsState()
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val onlineCount = allWorkers.count { it.isOnline }
    val workerIds = allWorkers.map { it.id }.toSet()
    val pendingCount = tasks.count { it.userId in workerIds && !it.isCompleted }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Mi equipo",
            subtitle = "${currentUser?.name ?: "Supervisor"} · Sectores: ${currentUser?.managedCommunes?.joinToString().orEmpty().ifEmpty { "todos" }}"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        value = "${allWorkers.size}",
                        label = "Reponedores",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        value = "$onlineCount",
                        label = "En línea",
                        accent = MaterialTheme.statusColors.success,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        value = "$pendingCount",
                        label = "Tareas pend.",
                        accent = MaterialTheme.statusColors.warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setTeamSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por nombre o código de local") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setTeamSearchQuery("") }) {
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

            if (communes.size > 2) {
                item(key = "communes") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(communes) { commune ->
                            FilterChip(
                                selected = selectedCommune == commune,
                                onClick = { viewModel.setSelectedCommuneFilter(commune) },
                                label = { Text(commune) }
                            )
                        }
                    }
                }
            }

            if (teamUsers.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Default.PersonSearch,
                        title = "Sin reponedores",
                        message = "No hay reponedores que coincidan con la búsqueda en tu sector."
                    )
                }
            } else {
                items(teamUsers, key = { it.id }) { user ->
                    TeamMemberCard(
                        user = user,
                        pendingTasks = tasks.count { it.userId == user.id && !it.isCompleted },
                        onClick = { selectedUserId = user.id }
                    )
                }
            }
        }
    }

    val selectedUser = selectedUserId?.let { id -> allWorkers.find { it.id == id } }
    if (selectedUser != null) {
        TeamMemberDetailDialog(
            user = selectedUser,
            tasks = tasks.filter { it.userId == selectedUser.id }.sortedBy { it.isCompleted },
            criticalProducts = restockProducts,
            viewModel = viewModel,
            onDismiss = { selectedUserId = null }
        )
    }
}

@Composable
fun TeamMemberCard(user: User, pendingTasks: Int, onClick: () -> Unit) {
    val status = MaterialTheme.statusColors
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserAvatar(user = user)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Local ${user.storeCode} · ${user.commune}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = user.scheduleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Pill(
                    text = if (user.isOnline) "En línea" else "Inactivo",
                    container = if (user.isOnline) status.successContainer else MaterialTheme.colorScheme.surfaceVariant,
                    content = if (user.isOnline) status.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pendingTasks > 0) {
                    Text(
                        text = if (pendingTasks == 1) "1 tarea" else "$pendingTasks tareas",
                        style = MaterialTheme.typography.labelSmall,
                        color = status.warning
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    var feedback by remember { mutableStateOf<String?>(null) }
    // Se lee para recalcular los botones de cooldown cuando se envía una alerta
    val alertVersion by viewModel.alertStateVersion.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { UserAvatar(user = user, size = 56.dp) },
        title = { Text(user.name) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Local ${user.storeCode} · ${user.commune}\n${user.scheduleText} · ${if (user.isOnline) "En línea" else "Inactivo"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                feedback?.let { message ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.statusColors.successContainer),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.statusColors.onSuccessContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.statusColors.onSuccessContainer
                            )
                        }
                    }
                }

                HorizontalDivider()
                SectionTitle("Productos críticos · enviar prioridad")

                if (criticalProducts.isEmpty()) {
                    Text(
                        text = "No hay productos críticos en este momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    criticalProducts.take(5).forEach { product ->
                        val isOnCooldown = remember(alertVersion, product.id) {
                            viewModel.isAlertOnCooldown(user.id, product.id)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge(status = product.stockStatus)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = product.locationText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = {
                                    feedback = when (val result = viewModel.sendPriorityAlert(user, product)) {
                                        AlertResult.Sent -> "Alerta enviada: ${product.displayName}"
                                        is AlertResult.OnCooldown -> "Podrás reenviar en ${result.remainingMinutes} min."
                                    }
                                },
                                enabled = !isOnCooldown,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                if (!isOnCooldown) {
                                    Icon(Icons.Default.PriorityHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Text(if (isOnCooldown) "Enviado" else "Priorizar")
                            }
                        }
                    }
                }

                HorizontalDivider()
                SectionTitle("Tareas asignadas (${tasks.count { !it.isCompleted }} pendientes)")

                if (tasks.isEmpty()) {
                    Text(
                        text = "Sin tareas. Las tareas se eliminan automáticamente después de una semana.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    tasks.forEach { task ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.PriorityHigh,
                                contentDescription = if (task.isCompleted) "Completada" else "Pendiente",
                                tint = if (task.isCompleted) MaterialTheme.statusColors.success else MaterialTheme.statusColors.warning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it.take(140) },
                    label = { Text("Nueva tarea o nota") },
                    placeholder = { Text("Ej: Revisar fechas de vencimiento en lácteos") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                viewModel.assignTask(user, newTaskTitle)
                                feedback = "Tarea enviada a ${user.name}"
                                newTaskTitle = ""
                            },
                            enabled = newTaskTitle.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar tarea")
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
