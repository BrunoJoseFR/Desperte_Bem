package com.example.despertebem

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val ALARM_DURATION_MINUTES = 20
    private val TOTAL_STEPS = 120 // 120 steps for 20 minutes (one every 10 seconds)
    private val STEP_INTERVAL_MS = (ALARM_DURATION_MINUTES * 60 * 1000L) / TOTAL_STEPS

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification()
        playAlarmGradually()
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "alarm_service_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Desperte Bem Alarme",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Desperte Bem")
            .setContentText("O alarme está tocando gradualmente...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun playAlarmGradually() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(this@AlarmService, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            isLooping = true
            prepare()
            setVolume(0f, 0f)
            start()
        }

        serviceScope.launch {
            for (step in 1..TOTAL_STEPS) {
                delay(STEP_INTERVAL_MS)
                val volume = step.toFloat() / TOTAL_STEPS
                mediaPlayer?.setVolume(volume, volume)
                android.util.Log.d("AlarmService", "Aumentando volume: $volume")
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        super.onDestroy()
    }
}
