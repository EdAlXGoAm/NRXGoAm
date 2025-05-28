package com.edalxgoam.nrxgoam.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.edalxgoam.nrxgoam.data.Alarm
import com.edalxgoam.nrxgoam.ui.theme.NRXGoAmTheme
import java.util.*

@Composable
fun AlarmScreen(
    modifier: Modifier = Modifier,
    onStartMusic: () -> Unit,
    onStopMusic: () -> Unit,
    onSetAlarm: (String, String, String, Date) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onDuplicateAlarm: (Alarm) -> Unit,
    alarms: List<Alarm>
) {
    var isMusicPlaying by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Date()) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Alarmas Programadas",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(alarms) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onDelete = { onDeleteAlarm(alarm.id) },
                    onEdit = { 
                        editingAlarm = alarm
                        title = alarm.title
                        description = alarm.description
                        category = alarm.category
                        selectedDate = alarm.date
                        showDialog = true
                    },
                    onDuplicate = { onDuplicateAlarm(alarm) }
                )
            }
        }
        
        Button(
            onClick = { 
                editingAlarm = null
                title = ""
                description = ""
                category = ""
                selectedDate = Date()
                showDialog = true 
            },
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Text("Agregar nueva alarma")
        }
        
        Button(
            onClick = {
                if (isMusicPlaying) {
                    onStopMusic()
                    isMusicPlaying = false
                } else {
                    onStartMusic()
                    isMusicPlaying = true
                }
            }
        ) {
            Text(if (isMusicPlaying) "Detener alarma" else "Activar alarma")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                editingAlarm = null
            },
            title = { Text(if (editingAlarm != null) "Editar Alarma" else "Nueva Alarma") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                                            calendar.set(Calendar.MINUTE, minute)
                                            selectedDate = calendar.time
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Seleccionar fecha y hora")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank() && category.isNotBlank()) {
                            if (editingAlarm != null) {
                                onEditAlarm(editingAlarm!!.copy(
                                    title = title,
                                    description = description,
                                    category = category,
                                    date = selectedDate
                                ))
                            } else {
                                onSetAlarm(title, description, category, selectedDate)
                            }
                            title = ""
                            description = ""
                            category = ""
                            showDialog = false
                            editingAlarm = null
                        }
                    }
                ) {
                    Text(if (editingAlarm != null) "Actualizar" else "Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDialog = false
                    editingAlarm = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alarm.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    TextButton(onClick = onDuplicate) {
                        Text("Clonar")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar alarma")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar alarma")
                    }
                }
            }
            Text(
                text = alarm.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDateTime(alarm.date),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = alarm.category,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatDateTime(date: Date): String {
    val calendar = Calendar.getInstance().apply {
        time = date
    }
    
    val hour12 = calendar.get(Calendar.HOUR)
    val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    
    return String.format("%02d/%02d/%d %02d:%02d %s",
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.YEAR),
        if (hour12 == 0) 12 else hour12,
        calendar.get(Calendar.MINUTE),
        amPm
    )
}

@Preview(showBackground = true)
@Composable
fun AlarmScreenPreview() {
    NRXGoAmTheme {
        AlarmScreen(
            onStartMusic = {},
            onStopMusic = {},
            onSetAlarm = { _, _, _, _ -> },
            onDeleteAlarm = { _ -> },
            onEditAlarm = { _ -> },
            onDuplicateAlarm = { _ -> },
            alarms = emptyList()
        )
    }
} 