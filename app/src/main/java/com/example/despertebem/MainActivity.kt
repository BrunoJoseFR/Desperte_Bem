package com.example.despertebem

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.provider.Settings
import android.widget.TimePicker
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.core.content.ContextCompat

import kotlinx.coroutines.delay
import java.io.File
import java.util.Calendar

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val darkColorScheme = darkColorScheme(
                primary = Color(0xFFD0BCFF),
                secondary = Color(0xFFCCC2DC),
                tertiary = Color(0xFFEFB8C8),
                background = Color(0xFF1C1B1F),
                surface = Color(0xFF1C1B1F),
            )

            MaterialTheme(colorScheme = darkColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CriarAlarme(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriarAlarme(context: Context) {

    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }

    var recordingStarted by remember {
        mutableStateOf(false)
    }

    val ambientSounds = listOf("Nenhum", "Chuva", "Ondas", "Floresta")
    var selectedSound by remember { mutableStateOf(ambientSounds[0]) }
    var expanded by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                recordingStarted = true
            } else {
                Toast.makeText(
                    context,
                    "Permissão do microfone negada",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    if (recordingStarted) {
        MonitorandoTela(context, selectedSound)
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
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            AndroidView(
                modifier = Modifier.padding(16.dp),
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
        }

        Spacer(modifier = Modifier.height(24.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = "Som ambiente: $selectedSound",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ambientSounds.forEach { sound ->
                    DropdownMenuItem(
                        text = { Text(sound) },
                        onClick = {
                            selectedSound = sound
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }

                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                try {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(context, "Por favor permita o Despertar Bem.", Toast.LENGTH_LONG).show()
                        val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
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
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        ) {
            Text("Definir Alarme e Monitorar Sono")
        }
    }
}

@Composable
fun MonitorandoTela(context: Context, ambientSound: String) {
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        try {
            val outputFile = File(context.getExternalFilesDir(null), "recorded_audio.mp4")
            val mediaRecorder = MediaRecorder(context)
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setOutputFile(outputFile.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
        } catch (e: Exception) {
            Toast.makeText(context, "Falha na gravação: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Gravando",
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Monitorando seu sono...",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        
        Text(
            text = "Fique tranquilo, o alarme está definido.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (ambientSound != "Nenhum") {
            val soundResId = when (ambientSound) {
                "Chuva" -> R.raw.chuva
            //    "Ondas" -> R.raw.ondas
            //    "Floresta" -> R.raw.floresta
                else -> null
            }

            if (soundResId != null) {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Tocando som de $ambientSound",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Desligará automaticamente em 20 min",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.DarkGray
                )

                val mediaPlayer = remember { MediaPlayer.create(context, soundResId) }
                
                LaunchedEffect(Unit) {
                    mediaPlayer?.apply {
                        isLooping = true
                        start()
                        delay(20 * 60 * 1000)
                        if (isPlaying) stop()
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        mediaPlayer?.apply {
                            if (isPlaying) stop()
                            release()
                        }
                    }
                }
            }
        }
    }
}
