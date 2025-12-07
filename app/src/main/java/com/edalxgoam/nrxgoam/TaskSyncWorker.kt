package com.edalxgoam.nrxgoam

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.edalxgoam.nrxgoam.R
import com.edalxgoam.nrxgoam.repository.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneId

/**
 * Worker que sincroniza tareas de Firebase en segundo plano cada 5 minutos
 * Descarga las tareas activas y programa sus recordatorios como alarmas
 */
class TaskSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "task_sync_work"
        private const val PREFS_NAME = "task_sync_prefs"
        private const val KEY_LAST_SYNC = "last_sync_time"
        private const val KEY_ALARM_IDS = "programmed_alarm_ids"
        private const val NOTIFICATION_CHANNEL_ID = "task_sync_channel"
        private const val NOTIFICATION_ID = 2001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            println("TaskSyncWorker: Iniciando sincronización de tareas...")
            
            // Crear canal de notificación si no existe
            createNotificationChannel()
            
            // Verificar si Firebase está inicializado
            if (!FirebaseManager.isFirebaseInitialized()) {
                try {
                    FirebaseManager.initialize(context)
                } catch (e: Exception) {
                    println("TaskSyncWorker: Error al inicializar Firebase: ${e.message}")
                    showErrorNotification("Error al inicializar Firebase")
                    return@withContext Result.retry()
                }
            }

            // Obtener repositorios
            val authRepo = FirebaseManager.getAuthRepository(context)
            val taskRepo = FirebaseManager.getTaskRepository()

            // Verificar si el usuario está autenticado
            val userId = authRepo.getCurrentUserId()
            if (userId == null) {
                println("TaskSyncWorker: Usuario no autenticado, saltando sincronización")
                showNotification("Sincronización", "Usuario no autenticado - sincronización omitida", false)
                return@withContext Result.success()
            }

            println("TaskSyncWorker: Sincronizando tareas para usuario: $userId")

            // Descargar tareas activas desde Firebase
            val tasksResult = taskRepo.getActiveTasks(userId, true)
            
            if (tasksResult.isFailure) {
                val errorMsg = tasksResult.exceptionOrNull()?.message ?: "Error desconocido"
                println("TaskSyncWorker: Error al obtener tareas: $errorMsg")
                showErrorNotification("Error al obtener tareas: $errorMsg")
                return@withContext Result.retry()
            }

            val tasks = tasksResult.getOrNull() ?: emptyList()
            println("TaskSyncWorker: Descargadas ${tasks.size} tareas activas")

            // Limpiar alarmas anteriores programadas por el worker
            clearPreviousAlarms()

            // Programar nuevas alarmas para recordatorios
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val programmedAlarmIds = mutableSetOf<String>()

            tasks.forEach { task ->
                task.reminders.forEachIndexed { reminderIndex, reminderIso ->
                    try {
                        // Parsear ISO a Instant
                        val instant = when {
                            reminderIso.contains("Z") || reminderIso.contains("+") -> {
                                Instant.parse(reminderIso)
                            }
                            reminderIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) -> {
                                Instant.parse("${reminderIso}:00Z")
                            }
                            reminderIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) -> {
                                Instant.parse("${reminderIso}Z")
                            }
                            else -> {
                                Instant.parse(reminderIso)
                            }
                        }

                        val triggerAtMillis = instant.toEpochMilli()
                        
                        // Solo programar alarmas futuras
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

                            programmedAlarmIds.add(requestCode.toString())
                            println("TaskSyncWorker: Programada alarma para tarea '${task.name}' el ${java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(instant.atZone(java.time.ZoneId.systemDefault()))}")
                        }
                    } catch (e: Exception) {
                        println("TaskSyncWorker: Error procesando recordatorio: $reminderIso - ${e.message}")
                    }
                }
            }

            // Guardar IDs de las alarmas programadas y tiempo de sincronización
            saveAlarmIds(programmedAlarmIds)
            saveLastSyncTime()

            // Mostrar notificación de éxito
            val currentTime = DateTimeFormatter.ofPattern("HH:mm:ss")
                .format(Instant.now().atZone(ZoneId.systemDefault()))
            
            showNotification(
                "✅ Sincronización Exitosa", 
                "📱 ${tasks.size} tareas descargadas\n⏰ $currentTime\n🔔 ${programmedAlarmIds.size} recordatorios programados",
                true
            )

            println("TaskSyncWorker: Sincronización completada exitosamente. Programadas ${programmedAlarmIds.size} alarmas.")
            Result.success()

        } catch (e: Exception) {
            println("TaskSyncWorker: Error inesperado: ${e.message}")
            e.printStackTrace()
            showErrorNotification("Error inesperado: ${e.message}")
            Result.retry()
        }
    }

    /**
     * Limpia las alarmas programadas anteriormente por el worker
     */
    private fun clearPreviousAlarms() {
        try {
            val previousAlarmIds = getPreviousAlarmIds()
            if (previousAlarmIds.isNotEmpty()) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                
                previousAlarmIds.forEach { alarmIdString ->
                    try {
                        val alarmId = alarmIdString.toInt()
                        val intent = Intent(context, AlarmReceiver::class.java)
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            alarmId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.cancel(pendingIntent)
                    } catch (e: Exception) {
                        println("TaskSyncWorker: Error cancelando alarma $alarmIdString: ${e.message}")
                    }
                }
                
                println("TaskSyncWorker: Canceladas ${previousAlarmIds.size} alarmas anteriores")
            }
        } catch (e: Exception) {
            println("TaskSyncWorker: Error limpiando alarmas anteriores: ${e.message}")
        }
    }

    /**
     * Guarda los IDs de las alarmas programadas
     */
    private fun saveAlarmIds(alarmIds: Set<String>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putStringSet(KEY_ALARM_IDS, alarmIds)
                .apply()
        } catch (e: Exception) {
            println("TaskSyncWorker: Error guardando IDs de alarmas: ${e.message}")
        }
    }

    /**
     * Obtiene los IDs de las alarmas programadas anteriormente
     */
    private fun getPreviousAlarmIds(): Set<String> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getStringSet(KEY_ALARM_IDS, emptySet()) ?: emptySet()
        } catch (e: Exception) {
            println("TaskSyncWorker: Error obteniendo IDs de alarmas anteriores: ${e.message}")
            emptySet()
        }
    }

    /**
     * Guarda el tiempo de la última sincronización
     */
    private fun saveLastSyncTime() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            println("TaskSyncWorker: Error guardando tiempo de sincronización: ${e.message}")
        }
    }

    /**
     * Crea el canal de notificación para sincronización
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sincronización de Tareas"
            val descriptionText = "Notificaciones sobre la sincronización automática de tareas"
            val importance = NotificationManager.IMPORTANCE_LOW // Baja importancia para no molestar
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(false) // Sin vibración para sincronización
                enableLights(false) // Sin luces
                setShowBadge(false) // Sin badge en el icono de la app
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación de sincronización
     */
    private fun showNotification(title: String, message: String, isSuccess: Boolean) {
        try {
            val intent = Intent(context, com.edalxgoam.nrxgoam.MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(if (isSuccess) android.R.drawable.ic_dialog_info else android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_LOW) // Baja prioridad
                .setAutoCancel(true) // Se cierra al tocarla
                .setContentIntent(pendingIntent)
                .setTimeoutAfter(10000) // Se auto-elimina después de 10 segundos
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
        } catch (e: Exception) {
            println("TaskSyncWorker: Error mostrando notificación: ${e.message}")
        }
    }

    /**
     * Muestra una notificación de error
     */
    private fun showErrorNotification(errorMessage: String) {
        val currentTime = DateTimeFormatter.ofPattern("HH:mm:ss")
            .format(Instant.now().atZone(ZoneId.systemDefault()))
        
        showNotification(
            "❌ Error de Sincronización",
            "⏰ $currentTime\n❗ $errorMessage",
            false
        )
    }

} 