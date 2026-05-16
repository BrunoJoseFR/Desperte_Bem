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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.delay
import java.io.File
import java.util.Calendar


//Para guardar decibels gravados durante monitoramento
data class DecibelSample(
    val timeMillis: Long,
    val decibels: Float
)

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

//Tela inicial
@Composable
fun AlarmScreen(context: Context) {

    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }

    var currentScreen by remember {
        mutableStateOf("setup")
    }

    var targetAlarmTime by remember {
        mutableLongStateOf(0L)
    }

    var graphSamples by remember {
        mutableStateOf<List<DecibelSample>>(emptyList())
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                currentScreen = "recording"

            } else {

                Toast.makeText(
                    context,
                    "Microphone permission denied",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    when (currentScreen) {

        "recording" -> {

            BlankRecordingScreen(
                context = context,
                targetTimeMillis = targetAlarmTime,

                onFinished = {

                    graphSamples = it
                    currentScreen = "graph"
                }
            )

            return
        }

        "graph" -> {

            GraphScreen(

                samples = graphSamples,

                onReset = {

                    graphSamples = emptyList()
                    currentScreen = "setup"
                }
            )

            return
        }
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

                    targetAlarmTime = calendar.timeInMillis

                    when {

                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED -> {

                            currentScreen = "recording"
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
fun BlankRecordingScreen(
    context: Context,
    targetTimeMillis: Long,
    onFinished: (List<DecibelSample>) -> Unit
) {

    var recorder by remember {
        mutableStateOf<MediaRecorder?>(null)
    }

    val samples = remember {
        mutableStateListOf<DecibelSample>()
    }

    LaunchedEffect(Unit) {

        try {

            val outputFile = File(
                context.getExternalFilesDir(null),
                "recorded_audio.m4a"
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

            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() < targetTimeMillis) {

                delay(1000)

                val amplitude =
                    mediaRecorder.maxAmplitude

                val db =
                    if (amplitude > 0) {
                        (20 * kotlin.math.log10(
                            amplitude.toDouble()
                        )).toFloat()
                    } else {
                        0f
                    }

                samples.add(
                    DecibelSample(
                        System.currentTimeMillis() - startTime,
                        db
                    )
                )
            }

            mediaRecorder.stop()
            mediaRecorder.release()

            onFinished(samples)

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "Recording failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Button(

            onClick = {

                try {

                    recorder?.stop()
                    recorder?.release()

                } catch (_: Exception) {
                }

                onFinished(samples)
            }

        ) {

            Text("Skip")
        }
    }
}

@Composable
fun GraphScreen(
    samples: List<DecibelSample>,
    onReset: () -> Unit
) {

    val entries = samples.map {
        it.decibels
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Noise Graph",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Chart(

            chart = lineChart(),

            model = entryModelOf(
                *entries.toTypedArray()
            ),

            startAxis = rememberStartAxis(),

            bottomAxis = rememberBottomAxis(),

            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(

            onClick = {
                onReset()
            }

        ) {

            Text("New Alarm")
        }
    }
}