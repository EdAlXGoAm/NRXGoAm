package com.edalxgoam.nrxgoam

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.edalxgoam.nrxgoam.ui.screens.DownloadReelsActivity
import com.edalxgoam.nrxgoam.ui.screens.PantryActivity
import com.edalxgoam.nrxgoam.ui.screens.TaskActivity
import com.edalxgoam.nrxgoam.ui.theme.*
import java.util.*
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var alarmRepository: AlarmRepository
    
    // Estado de la burbuja flotante
    private var isBubbleEnabled = mutableStateOf(false)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido
            println("Permiso de notificaciones concedido")
        }
    }
    
    // Launcher para el permiso de overlay
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Verificar si el permiso fue otorgado después de regresar de la configuración
        if (Settings.canDrawOverlays(this)) {
            startFloatingBubbleService()
            isBubbleEnabled.value = true
            saveBubbleState(true)
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
        
        // Cargar estado de la burbuja flotante
        isBubbleEnabled.value = loadBubbleState()
        
        // Si la burbuja estaba activa y tenemos permiso, reiniciarla
        if (isBubbleEnabled.value && Settings.canDrawOverlays(this)) {
            startFloatingBubbleService()
        }
        
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
                        },
                        onDownloadReelsClick = {
                            val intent = Intent(this, DownloadReelsActivity::class.java)
                            startActivity(intent)
                        },
                        isBubbleEnabled = isBubbleEnabled.value,
                        onBubbleToggle = { enabled ->
                            toggleFloatingBubble(enabled)
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
    
    // Funciones para la burbuja flotante
    private fun toggleFloatingBubble(enable: Boolean) {
        if (enable) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingBubbleService()
                isBubbleEnabled.value = true
                saveBubbleState(true)
            } else {
                // Solicitar permiso de overlay
                requestOverlayPermission()
            }
        } else {
            stopFloatingBubbleService()
            isBubbleEnabled.value = false
            saveBubbleState(false)
        }
    }
    
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }
    
    private fun startFloatingBubbleService() {
        val intent = Intent(this, FloatingBubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun stopFloatingBubbleService() {
        val intent = Intent(this, FloatingBubbleService::class.java)
        stopService(intent)
    }
    
    private fun saveBubbleState(enabled: Boolean) {
        val prefs = getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("bubble_enabled", enabled).apply()
    }
    
    private fun loadBubbleState(): Boolean {
        val prefs = getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("bubble_enabled", false)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        stopMusicService()
    }
}

// Data class para representar cada aplicación/módulo del menú
data class AppMenuItem(
    val title: String,
    val description: String,
    val headerColor: Color,
    val buttonColor: Color,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val buttonText: String = "Abrir"
)

@Composable
fun MainMenu(
    modifier: Modifier = Modifier,
    onAlarmClick: () -> Unit,
    onPantryClick: () -> Unit,
    onTaskClick: () -> Unit,
    onDownloadReelsClick: () -> Unit = {},
    isBubbleEnabled: Boolean = false,
    onBubbleToggle: (Boolean) -> Unit = {}
) {
    val menuItems = listOf(
        AppMenuItem(
            title = "Alarmas",
            description = "Programa recordatorios y alarmas para no olvidar nada importante.",
            headerColor = CardBlue,
            buttonColor = CardBlue,
            onClick = onAlarmClick
        ),
        AppMenuItem(
            title = "Despensa",
            description = "Gestiona tu inventario de alimentos y productos del hogar.",
            headerColor = CardYellow,
            buttonColor = CardYellow,
            onClick = onPantryClick
        ),
        AppMenuItem(
            title = "Tareas",
            description = "Organiza tus tareas diarias y proyectos de manera eficiente.",
            headerColor = CardTeal,
            buttonColor = CardTeal,
            onClick = onTaskClick
        ),
        AppMenuItem(
            title = "Download Reels",
            description = "Descarga videos de Facebook, Instagram, YouTube y TikTok.",
            headerColor = CardPink,
            buttonColor = CardPink,
            onClick = onDownloadReelsClick
        ),
        AppMenuItem(
            title = "Finanzas",
            description = "Próximamente: Gestiona tus finanzas personales.",
            headerColor = CardGreen,
            buttonColor = CardGreen,
            onClick = {},
            enabled = false,
            buttonText = "Próximamente"
        ),
        AppMenuItem(
            title = "Notas",
            description = "Próximamente: Toma notas y guarda información importante.",
            headerColor = CardPurple,
            buttonColor = CardPurple,
            onClick = {},
            enabled = false,
            buttonText = "Próximamente"
        ),
        AppMenuItem(
            title = "Configuración",
            description = "Próximamente: Personaliza la app a tu gusto.",
            headerColor = CardGray,
            buttonColor = CardGray,
            onClick = {},
            enabled = false,
            buttonText = "Próximamente"
        )
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFE2E8F0)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Header de bienvenida
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido 👋",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Todas tus aplicaciones en un solo lugar",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
        
        // Control de burbuja flotante
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Burbuja flotante",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Acceso rápido sobre otras apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                Switch(
                    checked = isBubbleEnabled,
                    onCheckedChange = onBubbleToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CardBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD1D5DB)
                    )
                )
            }
        }
        
        // Grid de tarjetas
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(menuItems) { item ->
                AppCard(
                    title = item.title,
                    description = item.description,
                    headerColor = item.headerColor,
                    buttonColor = item.buttonColor,
                    onClick = item.onClick,
                    enabled = item.enabled,
                    buttonText = item.buttonText
                )
            }
        }
    }
}

@Composable
fun AppCard(
    title: String,
    description: String,
    headerColor: Color,
    buttonColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    buttonText: String = "Abrir"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = headerColor.copy(alpha = 0.3f),
                spotColor = headerColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!enabled) Modifier.background(Color.White.copy(alpha = 0.6f))
                    else Modifier
                )
        ) {
            // Header con color
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (enabled) headerColor else headerColor.copy(alpha = 0.5f)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
            
            // Contenido
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) Color(0xFF64748B) else Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 16.dp),
                    minLines = 2,
                    maxLines = 3
                )
                
                // Botón
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFD1D5DB),
                        disabledContentColor = Color(0xFF9CA3AF)
                    )
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

// Mantener los componentes legacy por compatibilidad
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
        MainMenu(
            onAlarmClick = {}, 
            onPantryClick = {}, 
            onTaskClick = {},
            onDownloadReelsClick = {},
            isBubbleEnabled = false,
            onBubbleToggle = {}
        )
    }
}