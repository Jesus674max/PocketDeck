package com.pmmpdeck

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.net.Inet4Address
import java.net.NetworkInterface

private val Bg1 = Color(0xFF0B1020); private val Bg2 = Color(0xFF1A1F3D)
private val Card = Color(0x1AFFFFFF); private val Green = Color(0xFF3DDC97)
private val Red = Color(0xFFFF5C7A); private val Violet = Color(0xFF8B7CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ContextCompat.registerReceiver(this, TermuxBridge.receiver, TermuxBridge.filter,
            ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Screen() } }
    }
    override fun onDestroy() { unregisterReceiver(TermuxBridge.receiver); super.onDestroy() }
}

fun localIp(): String = try {
    NetworkInterface.getNetworkInterfaces().toList()
        .sortedBy { if (it.name.startsWith("wlan")) 0 else 1 }
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress ?: "sin red"
} catch (e: Exception) { "sin red" }

@Composable
fun Screen() {
    val ctx = LocalContext.current
    var running by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var ip by remember { mutableStateOf(localIp()) }
    var cmd by remember { mutableStateOf("") }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(ctx, "com.termux.permission.RUN_COMMAND")
            != PackageManager.PERMISSION_GRANTED) perm.launch("com.termux.permission.RUN_COMMAND")
    }
    LaunchedEffect(Unit) {
        while (true) {
            TermuxBridge.status(ctx) { out, err ->
                if (out.isBlank()) {
                    error = if (err.isNotBlank()) err else "Termux no responde. Revisa el permiso y allow-external-apps."
                } else {
                    error = null
                    val lines = out.lines()
                    running = lines.first().trim() == "ON"
                    log = lines.drop(1).joinToString("\n").trimEnd()
                    if (running) busy = false
                }
            }
            ip = localIp()
            delay(3000)
        }
    }
    LaunchedEffect(busy) { if (busy) { delay(6000); busy = false } }

    val pulse = rememberInfiniteTransition(label = "p")
    val glow by pulse.animateFloat(1f, 1.12f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    val accent by animateColorAsState(if (running) Green else Red, label = "c")

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Bg1, Bg2)))
            .statusBarsPadding().navigationBarsPadding().imePadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PocketDeck", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Servidor PocketMine en tu celular", color = Color(0xFF9AA3C7), fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))

        // Botón de encendido
        Box(Modifier.size(170.dp).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) {
            if (running) TermuxBridge.stop(ctx) else { busy = true; TermuxBridge.start(ctx) }
        }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(170.dp).scale(if (running) glow else 1f).alpha(0.25f)
                .background(accent, CircleShape))
            Box(Modifier.size(130.dp).background(Brush.radialGradient(listOf(accent.copy(alpha = .35f), Bg1)), CircleShape)
                .border(3.dp, accent, CircleShape), contentAlignment = Alignment.Center) {
                if (busy) CircularProgressIndicator(color = accent, modifier = Modifier.size(48.dp))
                else Canvas(Modifier.size(54.dp)) {
                    val w = 6.dp.toPx()
                    drawArc(accent, -60f, 300f, false, Offset(w, w), Size(size.width - 2 * w, size.height - 2 * w),
                        style = Stroke(w, cap = StrokeCap.Round))
                    drawLine(accent, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height * .5f), w, StrokeCap.Round)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(if (busy) "Iniciando…" else if (running) "ENCENDIDO" else "APAGADO",
            color = accent, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Text(if (running) "Toca para detener (stop)" else "Toca para encender", color = Color(0xFF9AA3C7), fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        // Tarjeta de conexión
        Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(18.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Conéctate desde Minecraft", color = Color(0xFF9AA3C7), fontSize = 11.sp)
                Text("$ip : 19132", color = Color.White, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            if (running) TextButton(onClick = { TermuxBridge.kill(ctx) }) { Text("Forzar", color = Red) }
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Red, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().background(Red.copy(alpha = .12f), RoundedCornerShape(12.dp)).padding(10.dp))
        }
        Spacer(Modifier.height(12.dp))

        // Consola
        val scroll = rememberScrollState()
        LaunchedEffect(log) { scroll.scrollTo(scroll.maxValue) }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF070A14), RoundedCornerShape(18.dp))
            .border(1.dp, Violet.copy(alpha = .3f), RoundedCornerShape(18.dp)).padding(12.dp)) {
            Text(if (log.isBlank()) "La consola aparecerá aquí cuando el servidor esté encendido." else log,
                color = Color(0xFFB8F5D6), fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                lineHeight = 15.sp, modifier = Modifier.verticalScroll(scroll))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = cmd, onValueChange = { cmd = it }, enabled = running, singleLine = true,
            placeholder = { Text("Comando (ej: op tunombre)") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (cmd.isNotBlank()) { TermuxBridge.send(ctx, cmd); cmd = "" }
            }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet, unfocusedBorderColor = Color(0x33FFFFFF))
        )
    }
}
