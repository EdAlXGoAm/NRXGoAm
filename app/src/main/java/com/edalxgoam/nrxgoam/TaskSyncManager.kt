package com.edalxgoam.nrxgoam

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Clase para gestionar la sincronización automática de tareas en segundo plano
 */
object TaskSyncManager {

    /**
     * Inicia la sincronización periódica de tareas cada 1 minuto
     */
    fun startPeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requiere conexión a internet
            .setRequiresBatteryNotLow(false) // Permitir ejecución con batería baja
            .setRequiresCharging(false) // No requiere que esté cargando
            .setRequiresDeviceIdle(false) // No requiere que el dispositivo esté inactivo
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<TaskSyncWorker>(
            repeatInterval = 15, // NOTA: Android limita el mínimo a 15 minutos para trabajos periódicos
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 5, // Flexibilidad de 5 minutos
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag("task_sync")
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TaskSyncWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE, // Reemplazar el trabajo existente
            syncWorkRequest
        )

        println("TaskSyncManager: Sincronización periódica iniciada (cada 15 minutos)")
        
        // Programar también sincronizaciones frecuentes usando AlarmManager
        startFrequentSyncWithAlarm(context)
    }

    /**
     * Detiene la sincronización periódica
     */
    fun stopPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TaskSyncWorker.UNIQUE_WORK_NAME)
        stopFrequentSyncWithAlarm(context)
        println("TaskSyncManager: Sincronización periódica detenida")
    }

    /**
     * Ejecuta una sincronización inmediata (una sola vez)
     */
    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateWorkRequest = OneTimeWorkRequestBuilder<TaskSyncWorker>()
            .setConstraints(constraints)
            .addTag("task_sync_immediate")
            .build()

        WorkManager.getInstance(context).enqueue(immediateWorkRequest)
        println("TaskSyncManager: Sincronización inmediata solicitada")
    }

    /**
     * Verifica el estado de la sincronización periódica
     */
    fun getSyncStatus(context: Context): WorkInfo.State? {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(TaskSyncWorker.UNIQUE_WORK_NAME)
                .get()
            
            workInfos.firstOrNull()?.state
        } catch (e: Exception) {
            println("TaskSyncManager: Error obteniendo estado de sincronización: ${e.message}")
            null
        }
    }

    /**
     * Obtiene información sobre la última sincronización
     */
    fun getLastSyncInfo(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences("task_sync_prefs", Context.MODE_PRIVATE)
            val lastSyncTime = prefs.getLong("last_sync_time", 0L)
            
            if (lastSyncTime > 0) {
                val currentTime = System.currentTimeMillis()
                val diffMinutes = (currentTime - lastSyncTime) / 60000
                when {
                    diffMinutes < 1 -> "Hace menos de 1 minuto"
                    diffMinutes < 60 -> "Hace $diffMinutes minutos"
                    diffMinutes < 1440 -> "Hace ${diffMinutes / 60} horas"
                    else -> "Hace ${diffMinutes / 1440} días"
                }
            } else {
                "Nunca sincronizado"
            }
        } catch (e: Exception) {
            println("TaskSyncManager: Error obteniendo información de sincronización: ${e.message}")
            "Error obteniendo información"
        }
    }

    /**
     * Reinicia la sincronización (detiene y vuelve a iniciar)
     */
    fun restartPeriodicSync(context: Context) {
        stopPeriodicSync(context)
        // Pequeña pausa antes de reiniciar
        Thread.sleep(100)
        startPeriodicSync(context)
        println("TaskSyncManager: Sincronización reiniciada")
    }

    /**
     * Inicia sincronización frecuente usando AlarmManager (cada 1 minuto)
     * para superar las limitaciones de WorkManager
     */
    private fun startFrequentSyncWithAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskSyncReceiver::class.java).apply {
            action = TaskSyncReceiver.ACTION_SYNC
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1000, // Request code único para sincronización
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Programar primera alarma en 1 minuto
        val firstTriggerTime = System.currentTimeMillis() + (1 * 60 * 1000) // 1 minuto

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            firstTriggerTime,
            pendingIntent
        )

        println("TaskSyncManager: AlarmManager configurado para sincronizaciones cada 1 minuto")
    }

    /**
     * Programa la siguiente alarma (llamado desde TaskSyncReceiver)
     */
    fun scheduleNextAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskSyncReceiver::class.java).apply {
            action = TaskSyncReceiver.ACTION_SYNC
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Programar siguiente alarma en 1 minuto
        val nextTriggerTime = System.currentTimeMillis() + (1 * 60 * 1000)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerTime,
            pendingIntent
        )
    }

    /**
     * Detiene la sincronización por AlarmManager
     */
    private fun stopFrequentSyncWithAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskSyncReceiver::class.java).apply {
            action = TaskSyncReceiver.ACTION_SYNC
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        println("TaskSyncManager: AlarmManager detenido")
    }
} 