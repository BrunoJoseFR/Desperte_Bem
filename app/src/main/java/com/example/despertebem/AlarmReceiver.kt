package com.example.despertebem

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

//Cria a notificação do alarme e inicia o serviço foreground que toca o som gradualmente
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        //ID unico do canal de notificação
        val channelId = "alarm_channel"

        //ontem o serviço responsavel por gerenciar notificações do sistema
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        //Cria o canal de notificação
        val channel = NotificationChannel(
            channelId,
            "Alarm Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )

        //Registra o canal no sistema e caso já exista, será reutilizado
        notificationManager.createNotificationChannel(channel)

        //Monta a notificação que será exibida ao usuario quando o alarme for acionado
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Desperte Bem")
            .setContentText("Hora de acordar!")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        //Exibe a notificação
        notificationManager.notify(1, notification)

        // START GRADUAL ALARM SERVICE
        val serviceIntent = Intent(context, AlarmService::class.java)
        context.startForegroundService(serviceIntent)
    }
}
