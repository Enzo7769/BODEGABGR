package com.example.bodegalider.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.bodegalider.model.TaskItem
import com.example.bodegalider.ui.components.EmptyState
import com.example.bodegalider.ui.components.Pill
import com.example.bodegalider.ui.components.ScreenHeader
import com.example.bodegalider.ui.theme.statusColors
import com.example.bodegalider.ui.viewmodel.InventoryViewModel

@Composable
fun NotificationsScreen(viewModel: InventoryViewModel) {
    val tasks by viewModel.visibleTasks.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSupervisor = currentUser?.isSupervisor == true
    var showCompleted by rememberSaveable { mutableStateOf(false) }

    val pending = tasks.filter { !it.isCompleted }
    val completed = tasks.filter { it.isCompleted }
    val shown = if (showCompleted) completed else pending

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Avisos y tareas",
            subtitle = if (isSupervisor) "Seguimiento de las tareas enviadas a tu equipo"
            else "Tareas de tu supervisor. Márcalas al terminarlas."
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showCompleted,
                onClick = { showCompleted = false },
                label = { Text("Pendientes (${pending.size})") }
            )
            FilterChip(
                selected = showCompleted,
                onClick = { showCompleted = true },
                label = { Text("Completadas (${completed.size})") }
            )
        }

        if (shown.isEmpty()) {
            EmptyState(
                icon = if (showCompleted) Icons.Default.NotificationsNone else Icons.Default.DoneAll,
                tint = if (showCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.statusColors.success,
                title = if (showCompleted) "Sin tareas completadas" else "¡Estás al día!",
                message = if (showCompleted) "Aquí verás las tareas terminadas de la última semana."
                else "No hay tareas pendientes por ahora.",
                modifier = Modifier.padding(top = 48.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shown, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        showAssignee = isSupervisor,
                        // Solo el reponedor confirma sus tareas; el supervisor hace seguimiento
                        onToggle = if (isSupervisor) null else ({ viewModel.toggleTaskCompleted(task.id) })
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskItem, showAssignee: Boolean, onToggle: (() -> Unit)?) {
    val status = MaterialTheme.statusColors
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        task.timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    val containerColor = when {
        task.isCompleted -> MaterialTheme.colorScheme.surfaceContainerLow
        task.isPriority -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val cardContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = if (onToggle != null) ({ onToggle() }) else null,
                enabled = onToggle != null
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.isPriority && !task.isCompleted) {
                        Pill(
                            text = "PRIORIDAD",
                            container = MaterialTheme.colorScheme.error,
                            content = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (showAssignee) {
                        Text(
                            text = "${task.userName} · Local ${task.storeCode}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (showAssignee || task.isPriority) Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (task.isCompleted) "Completada · $relativeTime" else "Pendiente · $relativeTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.isCompleted) status.success else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (onToggle != null) {
        Card(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) { cardContent() }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) { cardContent() }
    }
}
