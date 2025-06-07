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
import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.res.painterResource
import com.edalxgoam.nrxgoam.R

@Composable
fun AlarmScreen(
    modifier: Modifier = Modifier,
    onStartMusic: () -> Unit,
    onStopMusic: () -> Unit,
    onSetAlarm: (String, String, String, Date, String?) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onDuplicateAlarm: (Alarm) -> Unit,
    onChangeRingtone: (Alarm, String?) -> Unit,
    alarms: List<Alarm>
) {
    var isMusicPlaying by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Date()) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var selectedRingtoneUri by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedRingtoneUri = uri?.toString()
        }
    }
    
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
        
        val upcomingAlarms = remember(alarms) { alarms.filter { it.date.after(Date()) }.sortedBy { it.date } }
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(upcomingAlarms) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onDelete = { onDeleteAlarm(alarm.id) },
                    onEdit = { 
                        editingAlarm = alarm
                        title = alarm.title
                        description = alarm.description
                        category = alarm.category
                        selectedDate = alarm.date
                        selectedRingtoneUri = alarm.ringtoneUri
                        showDialog = true
                    },
                    onDuplicate = { onDuplicateAlarm(alarm) },
                    onChangeRingtone = { newUri -> onChangeRingtone(alarm, newUri) }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Selecciona un sonido")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    selectedRingtoneUri?.let { Uri.parse(it) }
                                )
                            }
                            ringtoneLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedRingtoneUri?.let {
                                RingtoneManager.getRingtone(context, Uri.parse(it)).getTitle(context)
                            } ?: "Seleccionar sonido de alarma"
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { selectedRingtoneUri = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restablecer sonido predeterminado")
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
                                    ringtoneUri = selectedRingtoneUri,
                                    date = selectedDate
                                ))
                            } else {
                                onSetAlarm(title, description, category, selectedDate, selectedRingtoneUri)
                            }
                            title = ""
                            description = ""
                            category = ""
                            selectedRingtoneUri = null
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
    onDuplicate: () -> Unit,
    onChangeRingtone: (String?) -> Unit
) {
    val isReminder = alarm.category == "Recordatorio"
    val context = LocalContext.current
    var localRingtoneUri by remember(alarm.ringtoneUri) { mutableStateOf(alarm.ringtoneUri) }
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            localRingtoneUri = uri?.toString()
            onChangeRingtone(localRingtoneUri)
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReminder)
                Color(0xFFA5D6A7) // verde claro para recordatorios
            else MaterialTheme.colorScheme.surface
        ),
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
                    TextButton(onClick = { onDuplicate() }) {
                        Text("Clonar")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar alarma")
                    }
                    IconButton(onClick = { onDelete() }) {
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
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Selecciona un sonido")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, localRingtoneUri?.let { Uri.parse(it) })
                    }
                    ringtoneLauncher.launch(intent)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_alarm_placeholder),
                        contentDescription = "Cambiar sonido"
                    )
                }
                Text(
                    text = localRingtoneUri?.let { RingtoneManager.getRingtone(context, Uri.parse(it)).getTitle(context) } ?: "Que suene",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            onSetAlarm = { _, _, _, _, _ -> },
            onDeleteAlarm = { _ -> },
            onEditAlarm = { _ -> },
            onDuplicateAlarm = { _ -> },
            onChangeRingtone = { _, _ -> },
            alarms = emptyList()
        )
    }
} 