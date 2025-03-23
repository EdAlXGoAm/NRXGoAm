package com.edalxgoam.nrxgoam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MUSIC -> startMusic()
            ACTION_STOP_MUSIC -> stopMusic()
        }
        return START_STICKY
    }

    private fun startMusic() {
        if (mediaPlayer == null) {
            initializeMediaPlayer()
        }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun stopMusic() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
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

    private fun createNotification(): Notification {
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
            .setContentTitle("¡Alarma!")
            .setContentText("Toca para abrir la aplicación")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openAppPendingIntent, true)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_pause, "Detener", stopPendingIntent)
            .setContentIntent(openAppPendingIntent)
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
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_MUSIC = "com.edalxgoam.nrxgoam.action.START_MUSIC"
        const val ACTION_STOP_MUSIC = "com.edalxgoam.nrxgoam.action.STOP_MUSIC"
    }
} 