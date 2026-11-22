package com.example.bodegalider.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bodegalider.model.ScanRange
import com.example.bodegalider.model.combineDateAndTime
import com.example.bodegalider.model.formatDate
import com.example.bodegalider.model.formatTime
import com.example.bodegalider.model.localHourMinute
import com.example.bodegalider.model.utcDateMillis
import java.time.ZoneId

private enum class PickerTarget { FROM_DATE, FROM_TIME, TO_DATE, TO_TIME }

/** Elige un rango de fecha y hora ("desde" y "hasta") para filtrar productos por su escaneo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanRangeDialog(
    initial: ScanRange,
    onApply: (ScanRange) -> Unit,
    onDismiss: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    var from by rememberSaveable { mutableStateOf(initial.fromMillis) }
    var to by rememberSaveable { mutableStateOf(initial.toMillis) }
    var picker by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    val isValid = from <= to

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rango de escaneo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateTimeRow(
                    label = "Desde",
                    date = formatDate(from, zone),
                    time = formatTime(from, zone),
                    onDateClick = { picker = PickerTarget.FROM_DATE },
                    onTimeClick = { picker = PickerTarget.FROM_TIME }
                )
                DateTimeRow(
                    label = "Hasta",
                    date = formatDate(to, zone),
                    time = formatTime(to, zone),
                    onDateClick = { picker = PickerTarget.TO_DATE },
                    onTimeClick = { picker = PickerTarget.TO_TIME }
                )
                if (!isValid) {
                    Text(
                        text = "La fecha \"Desde\" debe ser anterior a \"Hasta\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(ScanRange(from, to)) }, enabled = isValid) { Text("Aplicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    when (val target = picker) {
        PickerTarget.FROM_DATE, PickerTarget.TO_DATE -> {
            val current = if (target == PickerTarget.FROM_DATE) from else to
            val state = rememberDatePickerState(initialSelectedDateMillis = utcDateMillis(current, zone))
            DatePickerDialog(
                onDismissRequest = { picker = null },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { day ->
                            val (hour, minute) = localHourMinute(current, zone)
                            val updated = combineDateAndTime(day, hour, minute, zone)
                            if (target == PickerTarget.FROM_DATE) from = updated else to = updated
                        }
                        picker = null
                    }) { Text("Aceptar") }
                },
                dismissButton = { TextButton(onClick = { picker = null }) { Text("Cancelar") } }
            ) {
                DatePicker(state = state)
            }
        }

        PickerTarget.FROM_TIME, PickerTarget.TO_TIME -> {
            val current = if (target == PickerTarget.FROM_TIME) from else to
            val (hour, minute) = localHourMinute(current, zone)
            val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
            AlertDialog(
                onDismissRequest = { picker = null },
                text = { TimePicker(state = state) },
                confirmButton = {
                    TextButton(onClick = {
                        val updated = combineDateAndTime(utcDateMillis(current, zone), state.hour, state.minute, zone)
                        if (target == PickerTarget.FROM_TIME) from = updated else to = updated
                        picker = null
                    }) { Text("Aceptar") }
                },
                dismissButton = { TextButton(onClick = { picker = null }) { Text("Cancelar") } }
            )
        }

        null -> Unit
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    date: String,
    time: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onDateClick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(date)
            }
            OutlinedButton(onClick = onTimeClick) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(time)
            }
        }
    }
}
