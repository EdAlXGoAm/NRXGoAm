package com.edalxgoam.nrxgoam

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.edalxgoam.nrxgoam.data.Alarm
import com.edalxgoam.nrxgoam.data.AlarmRepository
import com.edalxgoam.nrxgoam.ui.theme.NRXGoAmTheme
import java.util.*
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var alarmRepository: AlarmRepository
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido
            println("Permiso de notificaciones concedido")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        alarmRepository = AlarmRepository(this)
        
        // Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permiso ya concedido
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        
        setContent {
            NRXGoAmTheme {
                var alarms by remember { mutableStateOf(alarmRepository.getAllAlarms()) }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlarmScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStartMusic = { startMusicService() },
                        onStopMusic = { stopMusicService() },
                        onSetAlarm = { hour, minute -> 
                            scheduleAlarm(hour, minute)
                            alarms = alarmRepository.getAllAlarms()
                        },
                        onDeleteAlarm = { alarmId -> 
                            deleteAlarm(alarmId)
                            alarms = alarmRepository.getAllAlarms()
                        },
                        alarms = alarms
                    )
                }
            }
        }
    }

    private fun scheduleAlarm(hour: Int, minute: Int) {
        val alarm = Alarm(hour = hour, minute = minute)
        alarmRepository.saveAlarm(alarm)
        
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun deleteAlarm(alarmId: Long) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        alarmRepository.deleteAlarm(alarmId)
    }

    private fun startMusicService() {
        val intent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_START_MUSIC
        }
        startService(intent)
    }

    private fun stopMusicService() {
        val intent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_STOP_MUSIC
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        stopMusicService()
    }
}

@Composable
fun AlarmScreen(
    modifier: Modifier = Modifier,
    onStartMusic: () -> Unit,
    onStopMusic: () -> Unit,
    onSetAlarm: (Int, Int) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
    alarms: List<Alarm>
) {
    var isMusicPlaying by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    
    val context = LocalContext.current
    
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
                    onDelete = { onDeleteAlarm(alarm.id) }
                )
            }
        }
        
        Button(
            onClick = {
                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, hour: Int, minute: Int ->
                        selectedHour = hour
                        selectedMinute = minute
                        onSetAlarm(hour, minute)
                    },
                    selectedHour,
                    selectedMinute,
                    false
                )
                timePickerDialog.show()
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
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime12Hour(alarm.hour, alarm.minute),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar alarma")
            }
        }
    }
}

private fun formatTime12Hour(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    
    val hour12 = calendar.get(Calendar.HOUR)
    val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    
    return String.format("%02d:%02d %s", 
        if (hour12 == 0) 12 else hour12,
        minute,
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
            onSetAlarm = { _, _ -> },
            onDeleteAlarm = { _ -> },
            alarms = emptyList()
        )
    }
}