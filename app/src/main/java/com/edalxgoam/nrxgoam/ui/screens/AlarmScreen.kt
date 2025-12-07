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
import com.edalxgoam.nrxgoam.model.Project
import com.edalxgoam.nrxgoam.model.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import com.edalxgoam.nrxgoam.model.EnvironmentUtils.toComposeColor
import com.edalxgoam.nrxgoam.ui.theme.NRXGoAmTheme
import androidx.compose.ui.graphics.Brush
import com.edalxgoam.nrxgoam.ui.theme.AlarmCardBackground_Offline
import com.edalxgoam.nrxgoam.ui.theme.AlarmCardBackground_Cloud
import com.edalxgoam.nrxgoam.ui.theme.AlarmCardBackground_Offline_Passed
import com.edalxgoam.nrxgoam.ui.theme.AlarmCardBackground_Cloud_Passed
import com.edalxgoam.nrxgoam.ui.theme.AlarmCardContrast_Offline
import com.edalxgoam.nrxgoam.ui.theme.AlarmCardContrast_Cloud

// Constantes para SharedPreferences del filtro de alarmas
private const val ALARM_PREFS_NAME = "alarm_screen_preferences"
private const val KEY_SHOW_HIDDEN = "show_hidden_alarms"

// Función para guardar el estado del filtro
private fun saveShowHiddenPreference(context: android.content.Context, showHidden: Boolean) {
    val prefs = context.getSharedPreferences(ALARM_PREFS_NAME, android.content.Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_SHOW_HIDDEN, showHidden).apply()
}

// Función para cargar el estado del filtro
private fun loadShowHiddenPreference(context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences(ALARM_PREFS_NAME, android.content.Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_SHOW_HIDDEN, false) // Por defecto false (no mostrar ocultas)
}

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
    alarms: List<Alarm>,
    projects: List<Project>,
    environments: List<Environment>
) {
    var isMusicPlaying by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alarma") }
    var selectedDate by remember { mutableStateOf(Date()) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var selectedRingtoneUri by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    var showHidden by remember { mutableStateOf(loadShowHiddenPreference(context)) }
    val environmentsSorted = remember(environments) { environments.sortedBy { it.name } }
    val projectsByEnv = remember(environmentsSorted, projects) {
        environmentsSorted.associateWith { env ->
            projects.filter { it.environment == env.id }.sortedBy { it.name }
        }
    }
    var expandedCategory by remember { mutableStateOf(false) }
    
    val calendar = Calendar.getInstance()
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedRingtoneUri = uri?.toString()
        }
    }
    
    val now = Date()
    val displayedAlarms = remember(alarms, showHidden) {
        alarms.filter { alarm ->
            if (showHidden) true // Mostrar todas las alarmas (programadas + ocultas)
            else alarm.date.after(now) // Solo mostrar alarmas futuras
        }.sortedBy { it.date }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (showHidden) "Todas las Alarmas" else "Alarmas Programadas",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Incluir alarmas ocultas")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = showHidden,
                onCheckedChange = { newValue ->
                    showHidden = newValue
                    saveShowHiddenPreference(context, newValue)
                }
            )
        }
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(displayedAlarms) { alarm ->
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
                category = "Alarma"
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
                    Text(text = "Categoría", style = MaterialTheme.typography.bodyMedium)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedCategory = true }
                                .padding(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DropdownMenuItem(
                                text = { Text("Alarma") },
                                onClick = {
                                    category = "Alarma"
                                    expandedCategory = false
                                }
                            )
                            Divider()
                            environmentsSorted.forEach { env ->
                                DropdownMenuItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(env.toComposeColor().copy(alpha = 0.2f)),
                                    text = { Text("Entorno: ${env.name}", fontWeight = FontWeight.Bold) },
                                    enabled = false,
                                    onClick = { }
                                )
                                (projectsByEnv[env] ?: emptyList()).forEach { project ->
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(env.toComposeColor().copy(alpha = 0.1f)),
                                        text = { Text(project.name) },
                                        onClick = {
                                            category = "(Entorno: ${env.name}) ${project.name}"
                                            expandedCategory = false
                                        }
                                    )
                                }
                                Divider()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Fecha y Hora", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Previsualización de fecha clickeable
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    calendar.time = selectedDate
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            calendar.set(year, month, day)
                                            selectedDate = calendar.time
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Fecha",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDateOnly(selectedDate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        // Previsualización de hora clickeable
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    calendar.time = selectedDate
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
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Hora",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatTimeOnly(selectedDate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
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
                        if (title.isNotBlank() && category.isNotBlank()) {
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
                            category = "Alarma"
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
    val isCloudAlarm = alarm.isCloud
    val isPassed = alarm.date.before(Date()) // Verificar si la alarma ya pasó
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
            containerColor = when {
                isCloudAlarm && isPassed -> AlarmCardBackground_Cloud_Passed
                isCloudAlarm && !isPassed -> AlarmCardBackground_Cloud
                !isCloudAlarm && isPassed -> AlarmCardBackground_Offline_Passed
                else -> AlarmCardBackground_Offline
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Cintillo lateral izquierdo más grueso
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        if (isCloudAlarm) AlarmCardContrast_Cloud else AlarmCardContrast_Offline
                    )
            )
            // Contenido principal
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alarm.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isPassed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                           else MaterialTheme.colorScheme.onSurface
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
                modifier = Modifier.padding(vertical = 4.dp),
                color = if (isPassed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                       else MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDateTime(alarm.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPassed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = alarm.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPassed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                    color = if (isPassed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

private fun formatDateOnly(date: Date): String {
    val calendar = Calendar.getInstance().apply {
        time = date
    }
    
    return String.format("%02d/%02d/%d",
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.YEAR)
    )
}

private fun formatTimeOnly(date: Date): String {
    val calendar = Calendar.getInstance().apply {
        time = date
    }
    
    val hour12 = calendar.get(Calendar.HOUR)
    val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    
    return String.format("%02d:%02d %s",
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
            alarms = emptyList(),
            projects = emptyList(),
            environments = emptyList()
        )
    }
} 