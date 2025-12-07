package com.edalxgoam.nrxgoam

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*

/**
 * BroadcastReceiver que ejecuta sincronizaciones frecuentes usando AlarmManager
 * para superar las limitaciones de WorkManager en segundo plano
 */
class TaskSyncReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_SYNC = "com.edalxgoam.nrxgoam.ACTION_SYNC"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SYNC -> {
                println("TaskSyncReceiver: Ejecutando sincronización por AlarmManager")
                
                // Ejecutar sincronización inmediata usando WorkManager
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val immediateWorkRequest = OneTimeWorkRequestBuilder<TaskSyncWorker>()
                    .setConstraints(constraints)
                    .addTag("task_sync_alarm")
                    .build()

                WorkManager.getInstance(context).enqueue(immediateWorkRequest)
                
                // Reprogramar la siguiente alarma
                TaskSyncManager.scheduleNextAlarm(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                println("TaskSyncReceiver: Dispositivo reiniciado, reiniciando sincronización")
                // Reiniciar sincronización después del reinicio
                TaskSyncManager.startPeriodicSync(context)
            }
        }
    }
} 