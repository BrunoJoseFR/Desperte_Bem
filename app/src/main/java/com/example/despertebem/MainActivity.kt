package com.example.despertebem

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
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

import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.concurrent.TimeUnit

// Para guardar decibels gravados durante monitoramento
data class DecibelSample(
    val timeMillis: Long,
    val decibels: Float
)

private val AMBIENT_SOUNDS = listOf("Nenhum", "Chuva", "Ondas", "Floresta")
private const val SNORING_THRESHOLD_DB = 50f

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
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("setup") }
    var selectedSound by remember { mutableStateOf(AMBIENT_SOUNDS[0]) }
    var targetAlarmTime by remember { mutableLongStateOf(0L) }
    var graphSamples by remember { mutableStateOf<List<DecibelSample>>(emptyList()) }

    when (currentScreen) {
        "setup" -> {
            CriarAlarme(
                onAlarmSet = { time, sound ->
                    targetAlarmTime = time
                    selectedSound = sound
                    currentScreen = "monitoring"
                }
            )
        }
        "monitoring" -> {
            MonitorandoTela(
                ambientSound = selectedSound,
                targetTimeMillis = targetAlarmTime,
                onFinished = { samples ->
                    graphSamples = samples
                    currentScreen = "graph"
                }
            )
        }
        "graph" -> {
            GraphScreen(
                samples = graphSamples,
                onReset = {
                    currentScreen = "setup"
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriarAlarme(onAlarmSet: (Long, String) -> Unit) {
    val context = LocalContext.current
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }

    var selectedSound by remember { mutableStateOf(AMBIENT_SOUNDS[0]) }
    var expanded by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                // Permissão concedida, o alarme será definido no clique do botão
            } else {
                Toast.makeText(
                    context,
                    "Permissão do microfone negada",
                    Toast.LENGTH_LONG
                ).show()
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
                AMBIENT_SOUNDS.forEach { sound ->
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
                            onAlarmSet(calendar.timeInMillis, selectedSound)
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
fun MonitorandoTela(
    ambientSound: String,
    targetTimeMillis: Long,
    onFinished: (List<DecibelSample>) -> Unit
) {
    val context = LocalContext.current
    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    val samples = remember { mutableStateListOf<DecibelSample>() }
    val liveEntries = remember { mutableStateListOf<Float>() }
    val modelProducer = remember { ChartEntryModelProducer() }
    
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
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            
            audioRecord = recorder
            recorder.startRecording()

            val startTime = System.currentTimeMillis()
            val buffer = ShortArray(bufferSize)
            
            while (System.currentTimeMillis() < targetTimeMillis) {
                val readSize = recorder.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    var maxAmp = 0f
                    for (i in 0 until readSize) {
                        val absValue = kotlin.math.abs(buffer[i].toInt()).toFloat()
                        if (absValue > maxAmp) maxAmp = absValue
                    }

                    // Conversão para dB aproximado
                    val db = if (maxAmp > 0) {
                        (20 * kotlin.math.log10(maxAmp.toDouble() / 32767.0 * 100.0)).coerceAtLeast(0.0).toFloat()
                    } else 0f
                    
                    val currentTime = System.currentTimeMillis() - startTime
                    samples.add(DecibelSample(currentTime, db))
                    
                    liveEntries.add(db)
                    if (liveEntries.size > 60) {
                        liveEntries.removeAt(0)
                    }
                    modelProducer.setEntries(liveEntries.mapIndexed { index, value -> entryOf(index, value) })
                }
                delay(100)
            }
            
            recorder.stop()
            recorder.release()
            onFinished(samples)
          }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro no monitoramento: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioRecord?.apply {
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
        // 🌊 Onda animada em tempo real
        Chart(
            chart = lineChart(
                lines = listOf(
                    lineSpec(
                        lineColor = Color.Cyan,
                        lineThickness = 2.dp
                    )
                )
            ),
            chartModelProducer = modelProducer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nível de ruído: ${liveEntries.lastOrNull()?.toInt() ?: 0} dB",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Cyan
        )

        Spacer(modifier = Modifier.height(32.dp))

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

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                audioRecord?.stop()
                audioRecord?.release()
                onFinished(samples)
            }
        ) {
            Text("Pular / Ver Gráfico")
        }

        if (ambientSound != "Nenhum") {
            val soundResId = when (ambientSound) {
                "Chuva" -> R.raw.chuva
                "Floresta" -> R.raw.floresta
                "Ondas" -> R.raw.ondas
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

                val mediaPlayer = remember(soundResId) { MediaPlayer.create(context, soundResId) }
                
                LaunchedEffect(mediaPlayer) {
                    mediaPlayer?.apply {
                        isLooping = true
                        start()
                        delay(20 * 60 * 1000)
                        if (isPlaying) stop()
                    }
                }

                DisposableEffect(mediaPlayer) {
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

@Composable
fun GraphScreen(
    samples: List<DecibelSample>,
    onReset: () -> Unit
) {
    val entries = samples.map { it.decibels }
    
    // Lógica para detectar roncos: Amostras acima do limite
    val snoringSamples = samples.filter { it.decibels >= SNORING_THRESHOLD_DB }
    val snoringCount = snoringSamples.size
    
    // Estimativa de duração (baseado no intervalo de 100ms das amostras)
    val snoringDurationSeconds = snoringCount * 0.1f 
    val durationText = if (snoringDurationSeconds >= 60) {
        "${(snoringDurationSeconds / 60).toInt()} min e ${(snoringDurationSeconds % 60).toInt()} seg"
    } else {
        "${snoringDurationSeconds.toInt()} seg"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Análise do Sono",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Resumo de Ruídos",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Cyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pico de Ruído:", color = Color.Gray)
                    Text("${entries.maxOrNull()?.toInt() ?: 0} dB", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Eventos de Ronco:", color = Color.Gray)
                    Text("$snoringCount", color = if (snoringCount > 0) Color.Red else Color.Green, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Duração Total:", color = Color.Gray)
                    Text(durationText, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (entries.isNotEmpty()) {
            SimpleLineChart(
                data = entries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        } else {
            Text("Sem dados para exibir", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = onReset
        ) {
            Text("Voltar ao Início")
        }
    }
}

@Composable
fun SimpleLineChart(data: List<Float>, modifier: Modifier = Modifier) {
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 30f
            textAlign = android.graphics.Paint.Align.RIGHT
        }
    }

    Canvas(modifier = modifier.padding(start = 40.dp, bottom = 24.dp, end = 16.dp, top = 16.dp)) {
        val width = size.width
        val height = size.height
        val maxVal = 100f
        
        // --- Desenha a Grade Vertical (dB) ---
        val gridColor = Color.DarkGray.copy(alpha = 0.3f)
        val levels = listOf(0f, 25f, 50f, 75f, 100f)
        
        levels.forEach { level ->
            val y = height - (level / maxVal * height)
            drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(width, y))
            
            // Texto do Eixo Y (dB)
            drawContext.canvas.nativeCanvas.drawText(
                "${level.toInt()} dB",
                -10f,
                y + 10f,
                textPaint
            )
        }

        // --- Desenha a Grade Horizontal (Tempo) ---
        if (data.isNotEmpty()) {
            val totalPoints = data.size
            // Vamos desenhar 5 marcações de tempo
            val timeMarkers = 5
            val step = totalPoints / (timeMarkers - 1)
            
            val timePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 30f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            for (i in 0 until timeMarkers) {
                val pointIndex = (i * step).coerceAtMost(totalPoints - 1)
                val x = (pointIndex.toFloat() / (totalPoints - 1).toFloat()) * width
                
                // Linha vertical da grade
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, height))
                
                // Formatação de tempo (assumindo 100ms por sample do AudioRecord anterior)
                val totalMillis = pointIndex * 100L
                val timeLabel = if (totalMillis < 60000) {
                    "${totalMillis / 1000}s"
                } else {
                    val mins = TimeUnit.MILLISECONDS.toMinutes(totalMillis)
                    val secs = TimeUnit.MILLISECONDS.toSeconds(totalMillis) % 60
                    "${mins}m${secs}s"
                }

                drawContext.canvas.nativeCanvas.drawText(
                    timeLabel,
                    x,
                    height + 40f,
                    timePaint
                )
            }

            // --- Desenha a Linha de Dados ---
            val path = Path()
            val xStep = if (totalPoints > 1) width / (totalPoints - 1) else 0f
            
            data.forEachIndexed { index, value ->
                val x = index * xStep
                val y = height - (value / maxVal * height).coerceIn(0f, height)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            drawPath(
                path = path,
                color = Color.Cyan,
                style = Stroke(width = 4f)
            )
        }
    }
}
