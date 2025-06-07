package com.edalxgoam.nrxgoam

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NRXGoAm::AlarmWakeLock").apply {
                acquire(30*1000L)
            }
        }

        try {
            // Iniciar el servicio de música
            val serviceIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_START_MUSIC
                // Pasar los extras del intent original
                putExtra("alarm_id", intent.getLongExtra("alarm_id", -1))
                putExtra("alarm_title", intent.getStringExtra("alarm_title"))
                putExtra("alarm_description", intent.getStringExtra("alarm_description"))
                putExtra("alarm_category", intent.getStringExtra("alarm_category"))
                putExtra("alarm_ringtone", intent.getStringExtra("alarm_ringtone"))
            }
            ContextCompat.startForegroundService(context, serviceIntent)

            // Abrir la aplicación con flags adicionales
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            context.startActivity(launchIntent)
        } finally {
            wakeLock.release()
        }
    }
} 