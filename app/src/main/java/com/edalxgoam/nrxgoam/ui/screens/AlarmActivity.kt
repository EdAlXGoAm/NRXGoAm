package com.edalxgoam.nrxgoam.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.edalxgoam.nrxgoam.AlarmReceiver
import com.edalxgoam.nrxgoam.MusicService
import com.edalxgoam.nrxgoam.data.Alarm
import com.edalxgoam.nrxgoam.data.AlarmRepository
import com.edalxgoam.nrxgoam.ui.theme.NRXGoAmTheme
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.time.Instant
import com.edalxgoam.nrxgoam.repository.FirebaseManager
import com.edalxgoam.nrxgoam.model.Task

class AlarmActivity : ComponentActivity() {
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
                val contextCompose = LocalContext.current
                val authRepo = remember { FirebaseManager.getAuthRepository(contextCompose) }
                val taskRepo = remember { FirebaseManager.getTaskRepository() }
                var alarms by remember { mutableStateOf(alarmRepository.getAllAlarms()) }
                var reminderAlarms by remember { mutableStateOf<List<Alarm>>(emptyList()) }

                LaunchedEffect(authRepo) {
                    val userId = authRepo.getCurrentUserId()
                    if (userId != null) {
                        val result = taskRepo.getActiveTasks(userId, true)
                        if (result.isSuccess) {
                            val tasks = result.getOrNull().orEmpty()
                            val listRems = tasks.flatMapIndexed { _, task ->
                                task.reminders.mapIndexed { index, iso ->
                                    val instant = Instant.parse(
                                        when {
                                            iso.contains("Z") || iso.contains("+") -> iso
                                            iso.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) -> "$iso:00Z"
                                            iso.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) -> "${iso}Z"
                                            else -> iso
                                        }
                                    )
                                    Alarm(
                                        id = task.id.hashCode().toLong() + index,
                                        title = task.name,
                                        description = task.description,
                                        category = "Recordatorio",
                                        ringtoneUri = null,
                                        date = Date.from(instant)
                                    )
                                }
                            }
                            reminderAlarms = listRems
                        }
                    }
                }

                val allAlarms = alarms + reminderAlarms
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlarmScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStartMusic = { startMusicService() },
                        onStopMusic = { stopMusicService() },
                        onSetAlarm = { title, description, category, date, ringtoneUri -> 
                            scheduleAlarm(title, description, category, date, ringtoneUri)
                            alarms = alarmRepository.getAllAlarms()
                        },
                        onDeleteAlarm = { alarmId -> 
                            deleteAlarm(alarmId)
                            alarms = alarmRepository.getAllAlarms()
                        },
                        onEditAlarm = { alarm -> 
                            editAlarm(alarm)
                            alarms = alarmRepository.getAllAlarms()
                        },
                        onDuplicateAlarm = { alarm -> 
                            duplicateAlarm(alarm)
                            alarms = alarmRepository.getAllAlarms()
                        },
                        onChangeRingtone = { alarm, ringtoneUri ->
                            editAlarm(alarm.copy(ringtoneUri = ringtoneUri))
                            alarms = alarmRepository.getAllAlarms()
                        },
                        alarms = allAlarms
                    )
                }
            }
        }
    }

    private fun scheduleAlarm(title: String, description: String, category: String, date: Date, ringtoneUri: String?) {
        val alarm = Alarm(
            title = title,
            description = description,
            category = category,
            ringtoneUri = ringtoneUri,
            date = date
        )
        alarmRepository.saveAlarm(alarm)
        
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_title", alarm.title)
            putExtra("alarm_description", alarm.description)
            putExtra("alarm_category", alarm.category)
            putExtra("alarm_ringtone", ringtoneUri)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            date.time,
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

    private fun editAlarm(alarm: Alarm) {
        // Cancelar la alarma existente
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        // Eliminar la alarma antigua
        alarmRepository.deleteAlarm(alarm.id)
        
        // Crear la nueva alarma con los datos actualizados
        val newAlarm = alarm.copy(
            id = System.currentTimeMillis()
        )
        alarmRepository.saveAlarm(newAlarm)
        
        // Programar la nueva alarma
        val newIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", newAlarm.id)
            putExtra("alarm_title", newAlarm.title)
            putExtra("alarm_description", newAlarm.description)
            putExtra("alarm_category", newAlarm.category)
            putExtra("alarm_ringtone", newAlarm.ringtoneUri)
        }
        
        val newPendingIntent = PendingIntent.getBroadcast(
            this,
            newAlarm.id.toInt(),
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            newAlarm.date.time,
            newPendingIntent
        )
    }

    private fun duplicateAlarm(alarm: Alarm) {
        // Si clonamos un recordatorio, eliminamos la categoría para no resaltarlo
        val newCategory = if (alarm.category == "Recordatorio") "" else alarm.category
        val newAlarm = alarm.copy(
            id = System.currentTimeMillis(),
            title = "${alarm.title} (Copia)",
            date = Date(alarm.date.time),
            category = newCategory
        )
        alarmRepository.saveAlarm(newAlarm)
        
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", newAlarm.id)
            putExtra("alarm_title", newAlarm.title)
            putExtra("alarm_description", newAlarm.description)
            putExtra("alarm_category", newAlarm.category)
            putExtra("alarm_ringtone", newAlarm.ringtoneUri)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            newAlarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            newAlarm.date.time,
            pendingIntent
        )
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
    }
} 