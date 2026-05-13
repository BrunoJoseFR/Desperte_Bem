package com.example.despertebem

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.provider.Settings
import android.widget.TimePicker
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.core.content.ContextCompat

import java.io.File
import java.util.Calendar

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AlarmScreen(this)
            }
        }
    }
}

@Composable
fun AlarmScreen(context: Context) {

    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }

    var recordingStarted by remember {
        mutableStateOf(false)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                recordingStarted = true

            } else {

                Toast.makeText(
                    context,
                    "Microphone permission denied",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    if (recordingStarted) {
        BlankRecordingScreen(context)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Desperte Bem",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        AndroidView(
            factory = {

                TimePicker(it).apply {

                    setIs24HourView(true)

                    setOnTimeChangedListener { _, h, m ->

                        hour = h
                        minute = m
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(

            onClick = {

                val calendar = Calendar.getInstance().apply {

                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }

                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                val intent =
                    Intent(context, AlarmReceiver::class.java)

                val pendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                                PendingIntent.FLAG_IMMUTABLE
                    )

                val alarmManager =
                    context.getSystemService(
                        Context.ALARM_SERVICE
                    ) as AlarmManager

                try {

                    if (!alarmManager.canScheduleExactAlarms()) {

                        Toast.makeText(
                            context,
                            "Please allow exact alarms",
                            Toast.LENGTH_LONG
                        ).show()

                        val settingsIntent = Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        )

                        context.startActivity(settingsIntent)

                        return@Button
                    }

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )

                    when {

                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED -> {

                            recordingStarted = true
                        }

                        else -> {

                            permissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    }

                } catch (e: Exception) {

                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        ) {

            Text("Set Alarm")
        }
    }
}

@Composable
fun BlankRecordingScreen(context: Context) {

    var recorder by remember {
        mutableStateOf<MediaRecorder?>(null)
    }

    LaunchedEffect(Unit) {

        try {

            val outputFile = File(
                context.getExternalFilesDir(null),
                "recorded_audio.mp4"
            )

            val mediaRecorder = MediaRecorder(context)

            mediaRecorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )

            mediaRecorder.setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )

            mediaRecorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AAC
            )

            mediaRecorder.setOutputFile(
                outputFile.absolutePath
            )

            mediaRecorder.prepare()
            mediaRecorder.start()

            recorder = mediaRecorder

            Toast.makeText(
                context,
                "Recording started",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "Recording failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    DisposableEffect(Unit) {

        onDispose {

            recorder?.apply {

                try {
                    stop()
                } catch (_: Exception) {
                }

                release()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    )
}