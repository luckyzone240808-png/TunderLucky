package dev.tunderlucky.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.tunderlucky.accessibility.ControlState
import dev.tunderlucky.ai.ChatMsg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ------------------------------------------------------------------ root
@Composable
fun Root(vm: AssistantViewModel) {
    var splash by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize().background(C.bg)) {
        Crossfade(splash, animationSpec = tween(700), label = "root") { s ->
            if (s) Splash(vm.lowGfx) { splash = false } else MainScreen(vm)
        }
        vm.pending?.let { p ->
            AlertDialog(
                onDismissRequest = { vm.confirm(false) },
                title = { Text("Confirm action") },
                text = { Text(p.text) },
                confirmButton = { TextButton(onClick = { vm.confirm(true) }) { Text("Confirm") } },
                dismissButton = { TextButton(onClick = { vm.confirm(false) }) { Text("Cancel") } }
            )
        }
    }
}

// ------------------------------------------------------------------ splash
@Composable
fun Splash(low: Boolean, onDone: () -> Unit) {
    val prog = remember { Animatable(0f) }
    val logo = remember { Animatable(0f) }
    var line by remember { mutableIntStateOf(0) }
    val parts = remember { List(90) { Triple(Random(it).nextFloat() * 6.28f, 0.25f + Random(it + 99).nextFloat() * 0.75f, 1.5f + Random(it + 7).nextFloat() * 2.5f) } }
    LaunchedEffect(Unit) {
        launch { prog.animateTo(1f, tween(2600, easing = LinearEasing)) }
        delay(900); launch { logo.animateTo(1f, tween(1200)) }
        delay(900); line = 1
        delay(500); line = 2
        delay(900); line = 3
        delay(700); onDone()
    }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val maxR = size.maxDimension * 0.6f
            parts.forEachIndexed { i, (a, d, r) ->
                val t = prog.value
                val rad = maxR * d * (1f - t) * (1f - t) + 30f
                val ang = a + t * 2.2f
                drawCircle(if (i % 2 == 0) C.cyan else C.violet, r, Offset(center.x + cos(ang) * rad, center.y + sin(ang) * rad), alpha = (1f - t * 0.6f))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(logo.value)) {
            OrbView(low, Modifier.size(260.dp))
            Waveform(true, Modifier.fillMaxWidth(0.7f).height(36.dp))
            Spacer(Modifier.height(16.dp))
            Text("TUNDER LUCKY", style = TextStyle(brush = Brush.linearGradient(listOf(C.cyan, C.violet, C.gold)), fontSize = 30.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 6.sp))
            Text("PERSONAL AI SYSTEM", color = C.dim, fontSize = 13.sp, letterSpacing = 4.sp, modifier = Modifier.alpha(if (line >= 2) 1f else 0f))
            Spacer(Modifier.height(10.dp))
            Text("INITIALIZING...", color = C.cyan, fontSize = 12.sp, letterSpacing = 3.sp, modifier = Modifier.alpha(if (line >= 3) 1f else 0f))
        }
    }
}

// ------------------------------------------------------------------ main shell
@Composable
fun MainScreen(vm: AssistantViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(vm)
        Box(Modifier.weight(1f)) {
            when (tab) { 0 -> HomeTab(vm); 1 -> CodeTab(vm); 2 -> DownloadsTab(vm); 3 -> SystemTab(vm); else -> SettingsTab(vm) }
        }
        NavigationBar(containerColor = Color(0xFF0A0C14)) {
            listOf("⌂" to "Home", "</>" to "Code", "⇩" to "Files", "ⓘ" to "System", "⚙" to "Settings").forEachIndexed { i, (g, n) ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i },
                    icon = { Text(g, color = if (tab == i) C.cyan else C.dim, fontSize = 16.sp) },
                    label = { Text(n, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = C.cyan.copy(alpha = 0.15f), selectedTextColor = C.cyan, unselectedTextColor = C.dim))
            }
        }
    }
}

@Composable
fun TopBar(vm: AssistantViewModel) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("TUNDER LUCKY", style = TextStyle(brush = Brush.linearGradient(listOf(C.cyan, C.violet)), fontSize = 16.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp))
        Spacer(Modifier.weight(1f))
        if (ControlState.active) {
            Text("CONTROL ON", color = C.red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("STOP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(C.red).clickable { vm.stopAll() }.padding(horizontal = 14.dp, vertical = 6.dp))
        } else {
            Text("Control off", color = C.dim, fontSize = 11.sp)
        }
    }
}

// ------------------------------------------------------------------ home
@Composable
fun HomeTab(vm: AssistantViewModel) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    val state = rememberLazyListState()
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> vm.refresh(); if (ok) vm.voice.listen() }
    val vs = vm.voice.state

    LaunchedEffect(vs) {
        when (vs) {
            "Speaking" -> while (true) { OrbState.amp = 0.35f + 0.25f * sin(System.nanoTime() / 1.2e8).toFloat(); delay(50) }
            "Processing" -> OrbState.amp = 0.2f
            "Idle" -> OrbState.amp = 0f
        }
    }
    LaunchedEffect(vm.messages.size, vm.messages.lastOrNull()?.text?.length) {
        if (vm.messages.isNotEmpty()) state.scrollToItem(vm.messages.lastIndex)
    }
    val quick = listOf("YouTube" to "open youtube", "Chrome" to "open chrome", "Wi-Fi" to "open wifi settings", "Bluetooth" to "open bluetooth settings",
        "Display" to "open display settings", "Sound" to "open sound settings", "Apps" to "open app settings", "Torch" to "flashlight", "Time" to "what time is it", "Battery" to "battery")

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        OrbView(vm.lowGfx, Modifier.fillMaxWidth().height(170.dp))
        Waveform(vs == "Listening" || vs == "Speaking", Modifier.fillMaxWidth().height(30.dp))
        val dot = when (vs) { "Listening" -> C.green; "Processing" -> C.gold; "Speaking" -> C.violet; else -> C.dim }
        Text("● ${vs.uppercase()}  •  ${vm.status}", color = dot, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 4.dp))
        if (vm.voice.partial.isNotEmpty()) Text("“${vm.voice.partial}”", color = C.text, fontSize = 13.sp)
        if (vm.voice.lastError.isNotEmpty()) Text(vm.voice.lastError, color = C.red, fontSize = 12.sp)

        Glass(Modifier.weight(1f).fillMaxWidth()) {
            if (vm.messages.isEmpty())
                Text("Say or type a command: “open YouTube and search Python programming”, “scroll down”, “what time is it”.\nPhone control needs the Control switch (System tab).", color = C.dim, fontSize = 13.sp)
            LazyColumn(state = state, verticalArrangement = Arrangement.spacedBy(6.dp)) { items(vm.messages) { Bubble(it) } }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quick.forEach { (l, c) -> Pill(l) { vm.handleInput(c) } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Type a command…", color = C.dim) },
                singleLine = true, colors = fieldColors(), shape = RoundedCornerShape(24.dp))
            Spacer(Modifier.width(6.dp))
            Pill("Send") { if (input.isNotBlank()) { vm.handleInput(input); input = "" } }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(58.dp).clip(CircleShape)
                .background(Brush.linearGradient(if (vs == "Listening") listOf(C.red, C.violet) else listOf(C.cyan, C.blue)))
                .clickable {
                    if (vs == "Listening") vm.voice.stopListening()
                    else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.voice.listen()
                    else mic.launch(Manifest.permission.RECORD_AUDIO)
                }, contentAlignment = Alignment.Center) { Text(if (vs == "Listening") "■" else "🎤", fontSize = 22.sp, color = Color.White) }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
fun Bubble(m: ChatMsg) {
    val user = m.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Text(m.text, color = C.text, fontSize = 14.sp,
            modifier = Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(16.dp))
                .background(if (user) C.blue.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.07f)).padding(10.dp))
    }
}

// ------------------------------------------------------------------ code
private val KW = Regex("\\b(def|class|fun|val|var|if|else|elif|for|while|return|import|from|public|private|static|void|int|String|new|function|const|let|True|False|None|null|true|false|in|is|not|and|or|try|except|catch|package|print|println)\\b")
private val NUM = Regex("\\b\\d+(\\.\\d+)?\\b")
private val STR = Regex("\"[^\"\\n]*\"|'[^'\\n]*'")
private val CMT = Regex("(#.*|//.*)")

class SyntaxTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val b = AnnotatedString.Builder(s)
        fun mark(r: Regex, c: Color) { r.findAll(s).forEach { b.addStyle(SpanStyle(color = c), it.range.first, it.range.last + 1) } }
        mark(KW, C.cyan); mark(NUM, C.violet); mark(STR, C.gold); mark(CMT, Color(0xFF6B7C93))
        return TransformedText(b.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
fun CodeTab(vm: AssistantViewModel) {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf("Generate") }
    var prompt by remember { mutableStateOf("") }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.codeText.toByteArray()) }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.codeText = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()?.take(200_000) ?: ""
    }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Generate", "Explain", "Debug").forEach { Pill(it, if (mode == it) C.gold else C.dim) { mode = it } }
        }
        OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("Ask (Python, Java, Kotlin, HTML/CSS/JS)") }, colors = fieldColors(), maxLines = 3)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(if (vm.codeBusy) "Working…" else "Ask AI") { if (!vm.codeBusy) vm.codeAsk(mode, prompt) }
            Pill("Copy", C.violet) { (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("code", vm.codeText)) }
            Pill("Save", C.violet) { save.launch("code.txt") }
            Pill("Open", C.violet) { open.launch(arrayOf("text/*", "application/json", "application/xml")) }
        }
        Glass(Modifier.weight(1f).fillMaxWidth()) {
            Text("EDITOR", color = C.dim, fontSize = 10.sp)
            BasicTextField(vm.codeText, { vm.codeText = it }, Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                textStyle = TextStyle(color = C.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                visualTransformation = SyntaxTransformation(), cursorBrush = Brush.linearGradient(listOf(C.cyan, C.cyan)))
        }
        Glass(Modifier.weight(1f).fillMaxWidth()) {
            Row { Text("AI OUTPUT (not executed or tested)", color = C.dim, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("Use code ↑", color = C.cyan, fontSize = 11.sp, modifier = Modifier.clickable { vm.useOutputAsCode() }) }
            Text(vm.codeOutput.ifEmpty { "Requires cloud AI setup (Settings). The editor works without it." }, color = C.text,
                fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    }
}

// ------------------------------------------------------------------ downloads
@Composable
fun DownloadsTab(vm: AssistantViewModel) {
    var url by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Direct file URL (https://…)") }, singleLine = true, colors = fieldColors())
        Pill("Download (asks to confirm)") { vm.requestDownload(url) }
        Text("Files go to your Downloads folder via Android DownloadManager. Only direct links; no access controls are bypassed.", color = C.dim, fontSize = 12.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.downloads) { d ->
                Glass(Modifier.fillMaxWidth()) {
                    Text(d.name, color = C.text, fontSize = 14.sp, maxLines = 1)
                    Text("${d.status}  •  ${d.size}", color = if (d.failed) C.red else C.dim, fontSize = 12.sp)
                    LinearProgressIndicator(progress = { d.progress }, Modifier.fillMaxWidth().padding(vertical = 6.dp), color = C.cyan)
                    if (d.done) Pill("Open") { vm.openDownload(d.id) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ system
@Composable
fun SystemTab(vm: AssistantViewModel) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { while (true) { vm.refresh(); delay(5000) } }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    val s = vm.sys
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Glass(Modifier.fillMaxWidth()) {
            Text("SYSTEM", color = C.gold, fontSize = 12.sp)
            Text("Device: ${s.model}", color = C.text); Text(s.android, color = C.text)
            Text("Battery: ${s.battery}%${if (s.charging) " (charging)" else ""}", color = C.text)
            Text("Storage free: %.1f / %.1f GB".format(s.freeGb, s.totalGb), color = C.text)
            Text("Network: ${s.network}", color = C.text)
            Text("Assistant service: ${if (vm.axEnabled) "connected" else "not enabled"}", color = C.text)
        }
        Glass(Modifier.fillMaxWidth()) {
            Text("PHONE CONTROL", color = C.gold, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (ControlState.active) "ON — last: ${ControlState.last}" else "OFF", color = if (ControlState.active) C.red else C.dim, modifier = Modifier.weight(1f))
                Switch(checked = ControlState.active, onCheckedChange = { if (it) vm.requestControl() else vm.stopAll() })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("STOP", C.red) { vm.stopAll() }
                Pill("Accessibility settings") { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            Text("Never operates password/OTP fields. Asks before send/post/buy/delete/submit.", color = C.dim, fontSize = 12.sp)
        }
        Glass(Modifier.fillMaxWidth()) {
            Text("PERMISSIONS", color = C.gold, fontSize = 12.sp)
            PermRow("Microphone", "Push-to-talk voice commands only", vm.micGranted) { mic.launch(Manifest.permission.RECORD_AUDIO) }
            PermRow("Accessibility", "Lets Control tap/scroll/type when you ask", vm.axEnabled) { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            PermRow("Internet", "Cloud AI & downloads (granted at install)", true) {}
            PermRow("Storage", "Not requested: Downloads + file picker used instead", true) {}
        }
        Glass(Modifier.fillMaxWidth()) {
            Row { Text("ACTIVITY HISTORY", color = C.gold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("Clear", color = C.cyan, fontSize = 12.sp, modifier = Modifier.clickable { vm.clearActivity() }) }
            if (vm.activity.isEmpty()) Text("Nothing yet.", color = C.dim, fontSize = 12.sp)
            vm.activity.take(30).forEach { Text(it, color = C.dim, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
    }
}

@Composable
fun PermRow(name: String, why: String, ok: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(name, color = C.text, fontSize = 14.sp); Text(why, color = C.dim, fontSize = 11.sp) }
        if (ok) Text("✓ OK", color = C.green, fontSize = 12.sp) else Pill("Grant", C.gold, onFix)
    }
}

// ------------------------------------------------------------------ settings
@Composable
fun SettingsTab(vm: AssistantViewModel) {
    var endpoint by remember { mutableStateOf(vm.prefs.endpoint) }
    var model by remember { mutableStateOf(vm.prefs.model) }
    var key by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Glass(Modifier.fillMaxWidth()) {
            Text("VOICE LANGUAGE", color = C.gold, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Pill("English (India)", if (vm.lang == "en-IN") C.cyan else C.dim) { vm.setLang("en-IN") }
                Pill("हिन्दी", if (vm.lang == "hi-IN") C.cyan else C.dim) { vm.setLang("hi-IN") }
            }
            Text("Hinglish works best with English (India) or Hindi depending on your speech engine.", color = C.dim, fontSize = 11.sp)
        }
        Glass(Modifier.fillMaxWidth()) {
            Text("CLOUD AI (optional)", color = C.gold, fontSize = 12.sp)
            Text("Any OpenAI-compatible chat-completions API. Needs your own provider/key, internet, and may have usage limits or cost. Local commands work without it.", color = C.dim, fontSize = 11.sp)
            OutlinedTextField(endpoint, { endpoint = it }, Modifier.fillMaxWidth(), label = { Text("Endpoint (https://…/chat/completions)") }, singleLine = true, colors = fieldColors())
            OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Model name") }, singleLine = true, colors = fieldColors())
            OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text(if (vm.hasKey) "API key (saved — enter to replace)" else "API key") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = fieldColors())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Pill("Save") { vm.saveCloud(endpoint, model, key); key = "" }
                Pill("Test", C.violet) { vm.testApi() }
                if (vm.hasKey) Pill("Remove key", C.red) { vm.clearKey() }
            }
            Text("Status: ${vm.apiStatus}  •  requests this session: ${vm.apiRequests}", color = C.text, fontSize = 12.sp)
            Text("The key is encrypted with an Android Keystore key, but anyone with full control of the device could still extract it from the running app.", color = C.dim, fontSize = 11.sp)
        }
        Glass(Modifier.fillMaxWidth()) {
            Text("PRIVACY & PERFORMANCE", color = C.gold, fontSize = 12.sp)
            ToggleRow("Save conversation history locally", vm.saveHistory) { vm.setSaveHistory(it) }
            ToggleRow("Speak responses", vm.speakReplies) { vm.setSpeak(it) }
            ToggleRow("Low graphics mode (no OpenGL, restart splash to apply)", vm.lowGfx) { vm.setLowGfx(it) }
            Pill("Clear conversation history", C.red) { vm.clearHistory() }
        }
        Text("TUNDER LUCKY 1.0.0 • Cloud AI only sends the text you type/say to your configured endpoint. Screen content is never uploaded.", color = C.dim, fontSize = 11.sp)
    }
}

@Composable
fun ToggleRow(label: String, v: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = C.text, fontSize = 13.sp, modifier = Modifier.weight(1f)); Switch(v, onChange)
    }
}
