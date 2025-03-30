package com.edalxgoam.nrxgoam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val binder = MusicBinder()
    private var originalVolume: Int = 0
    private var volumeJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Guardar el volumen original
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MUSIC -> startMusic(intent)
            ACTION_STOP_MUSIC -> stopMusic()
        }
        return START_STICKY
    }

    private fun startMusic(intent: Intent) {
        if (mediaPlayer == null) {
            initializeMediaPlayer()
        }
        
        val alarmId = intent.getLongExtra("alarm_id", -1)
        val alarmTitle = intent.getStringExtra("alarm_title") ?: "¡Alarma!"
        val alarmDescription = intent.getStringExtra("alarm_description") ?: "Toca para abrir la aplicación"
        val alarmCategory = intent.getStringExtra("alarm_category") ?: "General"
        
        startForeground(alarmId.toInt(), createNotification(alarmTitle, alarmDescription, alarmCategory))
        
        // Iniciar el incremento gradual del volumen
        startVolumeIncrease()
    }

    private fun startVolumeIncrease() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        
        // Cancelar cualquier incremento de volumen en curso
        volumeJob?.cancel()
        
        // Iniciar el incremento gradual
        volumeJob = serviceScope.launch {
            var currentVolume = originalVolume
            val stepDuration = 500L // 500ms entre cada incremento
            val volumeStep = 1
            
            while (currentVolume < maxVolume) {
                currentVolume = (currentVolume + volumeStep).coerceAtMost(maxVolume)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    currentVolume,
                    0
                )
                kotlinx.coroutines.delay(stepDuration)
            }
        }
    }

    private fun stopMusic() {
        volumeJob?.cancel()
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        
        // Restaurar el volumen original
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            originalVolume,
            0
        )
        
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarma",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para las notificaciones de alarma"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, description: String, category: String): Notification {
        val stopIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_STOP_MUSIC
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openAppPendingIntent, true)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_pause, "Detener", stopPendingIntent)
            .setContentIntent(openAppPendingIntent)
            .setSubText(category)
            .build()
    }

    private fun initializeMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
            mediaPlayer?.apply {
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }

    companion object {
        private const val CHANNEL_ID = "AlarmChannel"
        const val ACTION_START_MUSIC = "com.edalxgoam.nrxgoam.action.START_MUSIC"
        const val ACTION_STOP_MUSIC = "com.edalxgoam.nrxgoam.action.STOP_MUSIC"
    }
} 