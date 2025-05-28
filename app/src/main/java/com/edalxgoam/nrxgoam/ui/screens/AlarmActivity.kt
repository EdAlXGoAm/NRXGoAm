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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                var alarms by remember { mutableStateOf(alarmRepository.getAllAlarms()) }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlarmScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStartMusic = { startMusicService() },
                        onStopMusic = { stopMusicService() },
                        onSetAlarm = { title, description, category, date -> 
                            scheduleAlarm(title, description, category, date)
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
                        alarms = alarms
                    )
                }
            }
        }
    }

    private fun scheduleAlarm(title: String, description: String, category: String, date: Date) {
        val alarm = Alarm(
            title = title,
            description = description,
            category = category,
            date = date
        )
        alarmRepository.saveAlarm(alarm)
        
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_title", alarm.title)
            putExtra("alarm_description", alarm.description)
            putExtra("alarm_category", alarm.category)
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
        val newAlarm = alarm.copy(
            id = System.currentTimeMillis(),
            title = "${alarm.title} (Copia)",
            date = Date(alarm.date.time)
        )
        alarmRepository.saveAlarm(newAlarm)
        
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", newAlarm.id)
            putExtra("alarm_title", newAlarm.title)
            putExtra("alarm_description", newAlarm.description)
            putExtra("alarm_category", newAlarm.category)
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