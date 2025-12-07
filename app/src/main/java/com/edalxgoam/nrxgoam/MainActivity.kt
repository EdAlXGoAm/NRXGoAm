package com.edalxgoam.nrxgoam

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.edalxgoam.nrxgoam.data.Alarm
import com.edalxgoam.nrxgoam.data.AlarmRepository
import com.edalxgoam.nrxgoam.repository.FirebaseManager
import com.edalxgoam.nrxgoam.ui.components.AlarmIconFromFirebase
import com.edalxgoam.nrxgoam.ui.components.PantryIconFromFirebase
import com.edalxgoam.nrxgoam.ui.screens.AlarmActivity
import com.edalxgoam.nrxgoam.ui.screens.PantryActivity
import com.edalxgoam.nrxgoam.ui.screens.TaskActivity
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
        
        // Inicializar Firebase
        try {
            FirebaseManager.initialize(this)
            println("Firebase inicializado correctamente")
            
            // Precargar iconos en background
            scope.launch {
                try {
                    FirebaseManager.preloadIcons(this@MainActivity)
                    println("Iconos precargados desde Firebase Storage")
                } catch (e: Exception) {
                    println("Error al precargar iconos: ${e.message}")
                }
            }
            
            // Iniciar sincronización automática de tareas cada 5 minutos
            TaskSyncManager.startPeriodicSync(this)
            
            // Ejecutar sincronización inmediata al abrir la app
            TaskSyncManager.triggerImmediateSync(this)
            
        } catch (e: Exception) {
            println("Error al inicializar Firebase: ${e.message}")
            e.printStackTrace()
        }
        
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainMenu(
                        modifier = Modifier.padding(innerPadding),
                        onAlarmClick = {
                            val intent = Intent(this, AlarmActivity::class.java)
                            startActivity(intent)
                        },
                        onPantryClick = {
                            val intent = Intent(this, PantryActivity::class.java)
                            startActivity(intent)
                        },
                        onTaskClick = {
                            val intent = Intent(this, TaskActivity::class.java)
                            startActivity(intent)
                        }
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
        stopMusicService()
    }
}

@Composable
fun MainMenu(
    modifier: Modifier = Modifier,
    onAlarmClick: () -> Unit,
    onPantryClick: () -> Unit,
    onTaskClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Menú Principal",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FirebaseMenuIcon(
                label = "Alarma",
                iconType = FirebaseIconType.ALARM,
                onClick = onAlarmClick
            )
            
            FirebaseMenuIcon(
                label = "Despensa",
                iconType = FirebaseIconType.PANTRY,
                onClick = onPantryClick
            )
            
            FirebaseMenuIcon(
                label = "Tareas",
                iconType = FirebaseIconType.TASK,
                onClick = onTaskClick
            )
            
            // Aquí puedes agregar más íconos de menú en el futuro
        }
    }
}

enum class FirebaseIconType {
    ALARM,
    PANTRY,
    TASK
}

@Composable
fun FirebaseMenuIcon(
    label: String,
    iconType: FirebaseIconType,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .width(100.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClick)
        ) {
            when (iconType) {
                FirebaseIconType.ALARM -> {
                    AlarmIconFromFirebase(
                        modifier = Modifier.size(68.dp),
                        contentDescription = label
                    )
                }
                FirebaseIconType.PANTRY -> {
                    PantryIconFromFirebase(
                        modifier = Modifier.size(68.dp),
                        contentDescription = label
                    )
                }
                FirebaseIconType.TASK -> {
                    // Usar el mismo ícono que Alarma para Tareas
                    AlarmIconFromFirebase(
                        modifier = Modifier.size(68.dp),
                        contentDescription = label
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MenuIcon(
    label: String,
    iconResourceId: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .width(100.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClick)
        ) {
            Image(
                painter = painterResource(id = iconResourceId),
                contentDescription = label,
                modifier = Modifier.size(68.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    NRXGoAmTheme {
        MainMenu(onAlarmClick = {}, onPantryClick = {}, onTaskClick = {})
    }
}