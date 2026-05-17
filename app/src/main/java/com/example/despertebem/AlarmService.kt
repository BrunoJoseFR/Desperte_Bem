package com.example.despertebem

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AlarmService : Service() {

    // Responsável por tocar o som do alarme
    private var mediaPlayer: MediaPlayer? = null

    // Escopo usado para executar tarefas assíncronas
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // Tempo total para aumentar o volume
    private val ALARM_DURATION_MINUTES = 20

    // Quantidade de aumentos de volume
    private val TOTAL_STEPS = 120

    // Intervalo entre cada aumento
    private val STEP_INTERVAL_MS =
        (ALARM_DURATION_MINUTES * 60 * 1000L) / TOTAL_STEPS

    // Serviço não usa bind
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Inicia notificação do serviço
        startForegroundServiceNotification()

        // Inicia o alarme gradual
        playAlarmGradually()

        return START_STICKY
    }

    private fun startForegroundServiceNotification() {

        val channelId = "alarm_service_channel"

        // Gerencia notificações do sistema
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Cria canal de notificação
        val channel = NotificationChannel(
            channelId,
            "Desperte Bem Alarme",
            NotificationManager.IMPORTANCE_HIGH
        )

        notificationManager.createNotificationChannel(channel)

        // Cria notificação exibida enquanto o serviço roda
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Desperte Bem")
            .setContentText("O alarme está tocando gradualmente...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)

            // Mantém a notificação fixa
            .setOngoing(true)
            .build()

        // Inicia serviço em foreground
        startForeground(1001, notification)
    }

    private fun playAlarmGradually() {

        // Configura o player de áudio
        mediaPlayer = MediaPlayer().apply {

            // Define o áudio como tipo alarme
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            // Usa som padrão de alarme do Android
            setDataSource(
                this@AlarmService,
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            )

            // Mantém o áudio repetindo
            isLooping = true

            // Prepara o áudio
            prepare()

            // Começa sem volume
            setVolume(0f, 0f)

            // Inicia reprodução
            start()
        }

        // Coroutine para aumentar o volume aos poucos
        serviceScope.launch {

            for (step in 1..TOTAL_STEPS) {

                // Espera antes do próximo aumento
                delay(STEP_INTERVAL_MS)

                // Calcula o novo volume
                val volume = step.toFloat() / TOTAL_STEPS

                // Atualiza volume
                mediaPlayer?.setVolume(volume, volume)

                // Mostra volume no Logcat
                android.util.Log.d(
                    "AlarmService",
                    "Aumentando volume: $volume"
                )
            }
        }
    }

    override fun onDestroy() {

        // Cancela tarefas em execução
        serviceScope.cancel()

        // Para o áudio
        mediaPlayer?.stop()

        // Libera memória do player
        mediaPlayer?.release()

        super.onDestroy()
    }
}
