package com.edalxgoam.nrxgoam.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edalxgoam.nrxgoam.R
import com.edalxgoam.nrxgoam.model.Task
import com.edalxgoam.nrxgoam.model.Environment
import com.edalxgoam.nrxgoam.model.Project
import com.edalxgoam.nrxgoam.model.TaskWithDetails
import com.edalxgoam.nrxgoam.model.TaskUtils.getStatusDisplayName
import com.edalxgoam.nrxgoam.model.TaskUtils.getPriorityDisplayName
import com.edalxgoam.nrxgoam.model.TaskUtils.toPriorityColor
import com.edalxgoam.nrxgoam.repository.AuthRepository
import com.edalxgoam.nrxgoam.repository.FirebaseManager
import com.edalxgoam.nrxgoam.repository.UserInfo
import com.edalxgoam.nrxgoam.ui.theme.NRXGoAmTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.edalxgoam.nrxgoam.AlarmReceiver
import com.edalxgoam.nrxgoam.TaskSyncManager

// Función utilitaria para formatear fechas
fun formatDateFromISO(isoString: String): String {
    return try {
        if (isoString.isEmpty()) return ""
        
        val instant = when {
            // Formato ISO completo: "2025-05-21T08:20:37.926Z"
            isoString.contains("Z") || isoString.contains("+") -> {
                Instant.parse(isoString)
            }
            // Formato ISO parcial: "2025-05-21T14:10" - interpretar como UTC y agregar zona horaria
            isoString.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) -> {
                Instant.parse("${isoString}:00Z")
            }
            // Formato ISO parcial con segundos: "2025-05-21T14:10:30" - interpretar como UTC
            isoString.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) -> {
                Instant.parse("${isoString}Z")
            }
            else -> {
                // Intentar parsear como está
                Instant.parse(isoString)
            }
        }
        
        // Convertir de UTC a zona horaria local del dispositivo
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        println("Error parsing date: $isoString - ${e.message}")
        "Fecha inválida"
    }
}

// Función para obtener fecha relativa (ej: "Hace 2 días")
fun getRelativeTime(isoString: String): String {
    return try {
        if (isoString.isEmpty()) return ""
        
        val instant = when {
            // Formato ISO completo: "2025-05-21T08:20:37.926Z"
            isoString.contains("Z") || isoString.contains("+") -> {
                Instant.parse(isoString)
            }
            // Formato ISO parcial: "2025-05-21T14:10" - interpretar como UTC y agregar zona horaria
            isoString.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) -> {
                Instant.parse("${isoString}:00Z")
            }
            // Formato ISO parcial con segundos: "2025-05-21T14:10:30" - interpretar como UTC
            isoString.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) -> {
                Instant.parse("${isoString}Z")
            }
            else -> {
                // Intentar parsear como está
                Instant.parse(isoString)
            }
        }
        
        val now = Instant.now()
        val diffMinutes = java.time.Duration.between(instant, now).toMinutes()
        
        when {
            diffMinutes < 1 -> "Ahora mismo"
            diffMinutes < 60 -> "Hace ${diffMinutes}m"
            diffMinutes < 1440 -> "Hace ${diffMinutes / 60}h"
            diffMinutes < 10080 -> "Hace ${diffMinutes / 1440}d"
            else -> formatDateFromISO(isoString).split(" ")[0] // Solo fecha sin hora
        }
    } catch (e: Exception) {
        println("Error parsing relative time: $isoString - ${e.message}")
        ""
    }
}

// Constantes para SharedPreferences
private const val PREFS_NAME = "task_preferences"
private const val KEY_ORDER_DESCENDING = "order_descending"

// Función para guardar el estado del orden
private fun saveOrderPreference(context: android.content.Context, isDescending: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_ORDER_DESCENDING, isDescending).apply()
}

// Función para cargar el estado del orden
private fun loadOrderPreference(context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_ORDER_DESCENDING, true) // Por defecto descendente
}

class TaskActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NRXGoAmTheme {
                TaskScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Repositorios
    val authRepo = remember { FirebaseManager.getAuthRepository(context) }
    val taskRepo = remember { FirebaseManager.getTaskRepository() }
    val environmentRepo = remember { FirebaseManager.getEnvironmentRepository() }
    val projectRepo = remember { FirebaseManager.getProjectRepository() }
    
    // Estados
    var currentUser by remember { mutableStateOf<UserInfo?>(null) }
    var isAuthenticated by remember { mutableStateOf(false) }
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var environments by remember { mutableStateOf<List<Environment>>(emptyList()) }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var tasksWithDetails by remember { mutableStateOf<List<TaskWithDetails>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isOrderDescending by remember { mutableStateOf(loadOrderPreference(context)) } // Cargar desde cache
    var isRealtimeEnabled by remember { mutableStateOf(true) } // Control para tiempo real
    
    // Variable mutable para el callback de carga de tareas
    var loadTasksCallback: (() -> Unit)? by remember { mutableStateOf(null) }
    
    // Función para combinar tareas con información de proyecto y ambiente
    val combineTasksWithDetails = remember {
        {
            println("DEBUG: Combining tasks with details")
            println("DEBUG: Tasks count: ${tasks.size}")
            println("DEBUG: Projects count: ${projects.size}")
            println("DEBUG: Environments count: ${environments.size}")
            
            tasksWithDetails = tasks.map { task ->
                println("DEBUG: Processing task '${task.name}' with project ID: '${task.project}', environment ID: '${task.environment}'")
                
                val project = if (task.project.isNotEmpty()) {
                    val foundProject = projects.find { it.id == task.project }
                    println("DEBUG: Found project for ID '${task.project}': ${foundProject?.name ?: "NOT FOUND"}")
                    foundProject
                } else null
                
                val environment = if (task.environment.isNotEmpty()) {
                    val foundEnv = environments.find { it.id == task.environment }
                    println("DEBUG: Found environment for ID '${task.environment}': ${foundEnv?.name ?: "NOT FOUND"}")
                    foundEnv
                } else {
                    // Si la tarea no tiene ambiente directo, intentar obtenerlo desde el proyecto
                    project?.let { proj ->
                        val envFromProject = environments.find { it.id == proj.environment }
                        println("DEBUG: Found environment from project: ${envFromProject?.name ?: "NOT FOUND"}")
                        envFromProject
                    }
                }
                
                TaskWithDetails(
                    task = task,
                    project = project,
                    environment = environment
                )
            }
            println("DEBUG: Combined ${tasksWithDetails.size} tasks with details")
        }
    }
    
    // Launcher para Google Sign-In
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        println("TaskActivity: SignIn result received - resultCode: ${result.resultCode}")
        
        when (result.resultCode) {
            ComponentActivity.RESULT_OK -> {
                scope.launch {
                    result.data?.let { data ->
                        println("TaskActivity: Processing sign-in data...")
                        val signInResult = authRepo.handleSignInResult(data)
                        
                        if (signInResult.isSuccess) {
                            val user = signInResult.getOrNull()
                            println("TaskActivity: Sign-in successful - User: ${user?.email}")
                            currentUser = authRepo.getUserInfo()
                            isAuthenticated = true
                            errorMessage = null
                            // El LaunchedEffect se encargará de cargar las tareas automáticamente
                        } else {
                            val error = signInResult.exceptionOrNull()
                            println("TaskActivity: Sign-in failed - Error: ${error?.message}")
                            errorMessage = "Error al iniciar sesión: ${error?.message}"
                        }
                    } ?: run {
                        println("TaskActivity: No data received from sign-in")
                        errorMessage = "Error: No se recibieron datos de autenticación"
                    }
                }
            }
            ComponentActivity.RESULT_CANCELED -> {
                println("TaskActivity: Sign-in was cancelled by user")
                errorMessage = "Inicio de sesión cancelado por el usuario"
            }
            else -> {
                println("TaskActivity: Sign-in failed with result code: ${result.resultCode}")
                errorMessage = "Error en el inicio de sesión (código: ${result.resultCode})"
            }
        }
    }
    
    // Función para cargar ambientes y proyectos
    val loadEnvironmentsAndProjects = remember {
        {
            val userId = authRepo.getCurrentUserId()
            println("DEBUG: Loading environments and projects for user: $userId")
            if (userId != null) {
                scope.launch {
                    // Cargar ambientes
                    println("DEBUG: Loading environments...")
                    val envResult = environmentRepo.getUserEnvironments(userId)
                    if (envResult.isSuccess) {
                        environments = envResult.getOrNull() ?: emptyList()
                        println("DEBUG: Loaded ${environments.size} environments:")
                        environments.forEach { env ->
                            println("DEBUG: Environment - ID: ${env.id}, Name: ${env.name}")
                        }
                        if (environments.isEmpty()) {
                            println("DEBUG: No environments found, creating default environment")
                            val defaultEnv = Environment(
                                id = "default",
                                userId = userId,
                                name = "Default Environment",
                                color = "#2196F3"
                            )
                            environmentRepo.createEnvironment(defaultEnv)
                        }
                    } else {
                        println("ERROR: Failed to load environments: ${envResult.exceptionOrNull()?.message}")
                    }
                    
                    // Cargar proyectos
                    println("DEBUG: Loading projects...")
                    val projResult = projectRepo.getUserProjects(userId)
                    if (projResult.isSuccess) {
                        projects = projResult.getOrNull() ?: emptyList()
                        println("DEBUG: Loaded ${projects.size} projects:")
                        projects.forEach { proj ->
                            println("DEBUG: Project - ID: ${proj.id}, Name: ${proj.name}, Environment: ${proj.environment}")
                        }
                    } else {
                        println("ERROR: Failed to load projects: ${projResult.exceptionOrNull()?.message}")
                    }
                    
                    // Combinar tareas con detalles
                    combineTasksWithDetails()
                }
            }
            Unit
        }
    }
    
    // Función para cargar tareas manualmente (fallback cuando tiempo real falla)
    val loadUserTasksManually = remember {
        {
            val userId = authRepo.getCurrentUserId()
            if (userId != null) {
                scope.launch {
                    isLoading = true
                    val result = taskRepo.getActiveTasks(userId, isOrderDescending)
                    isLoading = false
                    
                    if (result.isSuccess) {
                        tasks = result.getOrNull() ?: emptyList()
                        combineTasksWithDetails() // Actualizar detalles
                        errorMessage = null
                    } else {
                        errorMessage = "Error al cargar tareas: ${result.exceptionOrNull()?.message}"
                    }
                }
            }
            Unit
        }
    }
    
    // Función para refrescar tareas manualmente (incluye sincronización inmediata)
    val refreshTasks = remember {
        {
            scope.launch {
                isLoading = true
                
                // Ejecutar sincronización inmediata en segundo plano
                TaskSyncManager.triggerImmediateSync(context)
                
                // Simular pequeña demora para mostrar el loading
                kotlinx.coroutines.delay(500)
                
                if (isRealtimeEnabled) {
                    // En modo tiempo real, solo mostrar que se está refrescando
                    isLoading = false
                } else {
                    // En modo manual, recargar datos
                    loadUserTasksManually()
                }
            }
            Unit
        }
    }
    
    // Función para ordenar las tareas en memoria
    val sortTasks = remember {
        { taskList: List<Task>, descending: Boolean ->
            val comparator = compareByDescending<Task> { task ->
                // Priorizar tareas con fecha de inicio
                if (task.start.isNotEmpty()) {
                    try {
                        // Normalizar formato de fecha para comparación
                        val normalizedStart = when {
                            task.start.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) -> "${task.start}:00Z"
                            task.start.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) -> "${task.start}Z"
                            else -> task.start
                        }
                        java.time.Instant.parse(normalizedStart)
                    } catch (e: Exception) {
                        // Si falla el parsing, usar fecha de creación
                        try {
                            java.time.Instant.parse(task.createdAt)
                        } catch (e2: Exception) {
                            java.time.Instant.EPOCH
                        }
                    }
                } else {
                    // Si no tiene fecha de inicio, usar fecha de creación
                    try {
                        java.time.Instant.parse(task.createdAt)
                    } catch (e: Exception) {
                        java.time.Instant.EPOCH
                    }
                }
            }
            
            if (descending) {
                taskList.sortedWith(comparator)
            } else {
                taskList.sortedWith(comparator.reversed())
            }
        }
    }
    
    // Función para iniciar sesión con Google
    val signInWithGoogle = remember {
        {
            try {
                val signInIntent = authRepo.getSignInIntent()
                signInLauncher.launch(signInIntent)
            } catch (e: Exception) {
                errorMessage = "Error al iniciar Google Sign-In: ${e.message}"
            }
            Unit
        }
    }
    
    // Función para cerrar sesión
    val signOut = remember {
        {
            scope.launch {
                val result = authRepo.signOut()
                if (result.isSuccess) {
                    currentUser = null
                    isAuthenticated = false
                    tasks = emptyList()
                    tasksWithDetails = emptyList()
                    errorMessage = null
                } else {
                    errorMessage = "Error al cerrar sesión: ${result.exceptionOrNull()?.message}"
                }
            }
            Unit
        }
    }
    
    // Efecto para escuchar cambios en tiempo real de las tareas (con fallback)
    LaunchedEffect(isAuthenticated, authRepo, isRealtimeEnabled) {
        if (isAuthenticated && isRealtimeEnabled) {
            val userId = authRepo.getCurrentUserId()
            if (userId != null) {
                println("TaskActivity: Iniciando listener de tiempo real para user: $userId")
                isLoading = true
                
                try {
                    // Escuchar cambios en tiempo real
                    taskRepo.listenToActiveTasks(userId).collect { taskList ->
                        println("TaskActivity: Tareas actualizadas en tiempo real - Count: ${taskList.size}")
                        
                        // Si recibimos lista vacía y antes teníamos tareas, podría ser error de permisos
                        if (taskList.isEmpty() && tasks.isNotEmpty()) {
                            println("TaskActivity: Posible error de permisos detectado, cambiando a modo manual")
                            isRealtimeEnabled = false
                            loadUserTasksManually()
                            return@collect
                        }
                        
                        // Aplicar ordenamiento según preferencia
                        tasks = sortTasks(taskList, isOrderDescending)
                        combineTasksWithDetails() // Actualizar detalles
                        isLoading = false
                        errorMessage = null
                    }
                } catch (e: Exception) {
                    println("TaskActivity: Error en tiempo real, cambiando a modo manual: ${e.message}")
                    isRealtimeEnabled = false
                    loadUserTasksManually()
                }
            }
        } else if (isAuthenticated && !isRealtimeEnabled) {
            // Modo manual como fallback
            loadUserTasksManually()
        } else {
            tasks = emptyList()
            tasksWithDetails = emptyList()
            isLoading = false
        }
    }
    
    // Efecto para cargar ambientes y proyectos cuando el usuario se autentique
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            loadEnvironmentsAndProjects()
        } else {
            environments = emptyList()
            projects = emptyList()
            tasksWithDetails = emptyList()
        }
    }
    
    // Efecto para combinar tareas con detalles cuando cambien las tareas
    LaunchedEffect(tasks, projects, environments) {
        if (tasks.isNotEmpty() && (projects.isNotEmpty() || environments.isNotEmpty())) {
            combineTasksWithDetails()
        }
    }
    
    // Verificar estado de autenticación al iniciar
    LaunchedEffect(Unit) {
        currentUser = authRepo.getUserInfo()
        isAuthenticated = authRepo.isUserAuthenticated()
    }
    
    // Efecto para reordenar cuando cambia la preferencia de orden
    LaunchedEffect(isOrderDescending) {
        if (tasks.isNotEmpty()) {
            tasks = sortTasks(tasks, isOrderDescending)
            combineTasksWithDetails() // Actualizar detalles después de reordenar
        }
    }
    
    // Efecto para programar alarmas en las fechas de reminders de las tareas
    LaunchedEffect(tasks) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        tasks.forEachIndexed { taskIndex, task ->
            task.reminders.forEachIndexed { reminderIndex, reminderIso ->
                try {
                    // Parsear ISO a Instant
                    val instant = when {
                        reminderIso.contains("Z") || reminderIso.contains("+") -> Instant.parse(reminderIso)
                        reminderIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) -> Instant.parse("$reminderIso:00Z")
                        reminderIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) -> Instant.parse("${reminderIso}Z")
                        else -> Instant.parse(reminderIso)
                    }
                    val triggerAtMillis = instant.toEpochMilli()
                    if (triggerAtMillis > System.currentTimeMillis()) {
                        val requestCode = task.id.hashCode() + reminderIndex
                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra("alarm_id", requestCode.toLong())
                            putExtra("alarm_title", task.name)
                            putExtra("alarm_description", task.description)
                            putExtra("alarm_category", "Recordatorio")
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } catch (e: Exception) {
                    println("Error programando reminder: $reminderIso - ${e.message}")
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tareas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isAuthenticated) {
                        IconButton(onClick = { 
                            isOrderDescending = !isOrderDescending
                            saveOrderPreference(context, isOrderDescending)
                            if (isRealtimeEnabled) {
                                // En tiempo real, solo reordenar la lista actual
                                if (tasks.isNotEmpty()) {
                                    tasks = sortTasks(tasks, isOrderDescending)
                                }
                            } else {
                                // En modo manual, recargar con nuevo orden
                                loadUserTasksManually()
                            }
                        }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_swap_vert), 
                                contentDescription = if (isOrderDescending) "Ordenar más antiguo primero" else "Ordenar más reciente primero",
                                tint = if (isOrderDescending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = refreshTasks) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                        }
                        IconButton(onClick = signOut) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                !isAuthenticated -> {
                    AuthenticationScreen(
                        onSignInClick = signInWithGoogle,
                        errorMessage = errorMessage
                    )
                }
                
                else -> {
                    TaskListScreen(
                        user = currentUser,
                        tasksWithDetails = tasksWithDetails,
                        onRefresh = refreshTasks,
                        errorMessage = errorMessage,
                        isRealtimeEnabled = isRealtimeEnabled
                    )
                }
            }
        }
    }
}

@Composable
fun AuthenticationScreen(
    onSignInClick: () -> Unit,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔐",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Iniciar Sesión",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Inicia sesión con tu cuenta de Google para ver tus tareas",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onSignInClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Iniciar sesión con Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun TaskListScreen(
    user: UserInfo?,
    tasksWithDetails: List<TaskWithDetails>,
    onRefresh: () -> Unit,
    errorMessage: String?,
    isRealtimeEnabled: Boolean
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header del usuario
        user?.let { userInfo ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInfo.displayName.firstOrNull()?.toString() ?: "?",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = userInfo.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = userInfo.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Indicador de estado de sincronización
                        val lastSyncInfo = remember(key1 = context) { TaskSyncManager.getLastSyncInfo(context) }
                        Text(
                            text = if (isRealtimeEnabled) "🔄 Sincronización automática" else "📱 Modo manual",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRealtimeEnabled) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "⏰ Última sync en segundo plano: $lastSyncInfo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
        
        // Mensaje de error
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Lista de tareas
        if (tasksWithDetails.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📝",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "No hay tareas activas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onRefresh) {
                        Text("Actualizar")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasksWithDetails) { taskWithDetails ->
                    TaskItem(taskWithDetails = taskWithDetails)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recordatorios descargados:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = tasksWithDetails.flatMap { it.task.reminders }.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TaskItem(taskWithDetails: TaskWithDetails) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Aquí se puede implementar navegación a detalle */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Emoji, Título y Prioridad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = taskWithDetails.task.emoji.ifEmpty { "📝" },
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    Column {
                        Text(
                            text = taskWithDetails.task.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (taskWithDetails.task.description.isNotEmpty()) {
                            Text(
                                text = taskWithDetails.task.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                
                // Prioridad
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(taskWithDetails.task.toPriorityColor().copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = taskWithDetails.task.getPriorityDisplayName(),
                        style = MaterialTheme.typography.labelSmall,
                        color = taskWithDetails.task.toPriorityColor(),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Información de fechas
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Fecha de creación (siempre visible)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_calendar_today),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Creada: ${getRelativeTime(taskWithDetails.task.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Fecha de vencimiento (si existe)
                if (!taskWithDetails.task.deadline.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_schedule),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFF6B35) // Color naranja para deadline
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Vence: ${formatDateFromISO(taskWithDetails.task.deadline)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF6B35),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Fechas de inicio y fin (si existen)
                if (taskWithDetails.task.start.isNotEmpty() || taskWithDetails.task.end.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_schedule),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = buildString {
                                if (taskWithDetails.task.start.isNotEmpty()) {
                                    append("Inicio: ${formatDateFromISO(taskWithDetails.task.start)}")
                                }
                                if (taskWithDetails.task.end.isNotEmpty()) {
                                    if (taskWithDetails.task.start.isNotEmpty()) append(" • ")
                                    append("Fin: ${formatDateFromISO(taskWithDetails.task.end)}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Fecha de completado (si está completada)
                if (taskWithDetails.task.completed && !taskWithDetails.task.completedAt.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Green
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Completada: ${getRelativeTime(taskWithDetails.task.completedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Información adicional (Estado y Proyecto)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Estado con color
                val statusColor = when (taskWithDetails.task.status) {
                    "completed" -> Color.Green
                    "in-progress" -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                
                Text(
                    text = "Estado: ${taskWithDetails.task.getStatusDisplayName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = if (taskWithDetails.task.status != "pending") FontWeight.Medium else FontWeight.Normal
                )
            }
            
            // Nueva sección para ambiente y proyecto
            if (taskWithDetails.hasCompleteHierarchy() || taskWithDetails.project != null || taskWithDetails.environment != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Mostrar ambiente si existe
                    taskWithDetails.environment?.let { environment ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(taskWithDetails.getEnvironmentColor())
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🌐 ${environment.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Mostrar proyecto si existe
                    taskWithDetails.project?.let { project ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(taskWithDetails.getProjectColor())
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📁 ${project.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Mostrar jerarquía completa si ambos existen
                    if (taskWithDetails.hasCompleteHierarchy()) {
                        Text(
                            text = "📍 ${taskWithDetails.getHierarchyDescription()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (taskWithDetails.task.project.isNotEmpty()) {
                // Fallback: mostrar el ID del proyecto si no se encontró información
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "📁 ${taskWithDetails.task.project}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Mostrar recordatorios de esta tarea
            if (taskWithDetails.task.reminders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recordatorios:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    taskWithDetails.task.reminders.forEach { reminderIso ->
                        Text(
                            text = formatDateFromISO(reminderIso),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
} 