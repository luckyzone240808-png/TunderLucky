package dev.tunderlucky.ui

import android.accessibilityservice.AccessibilityService
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tunderlucky.accessibility.ControlState
import dev.tunderlucky.accessibility.LuckyAccessibilityService
import dev.tunderlucky.ai.ChatMsg
import dev.tunderlucky.ai.CloudClient
import dev.tunderlucky.automation.Launch
import dev.tunderlucky.browser.Chrome
import dev.tunderlucky.downloads.DownloadHelper
import dev.tunderlucky.security.SecureStore
import dev.tunderlucky.settings.Prefs
import dev.tunderlucky.system.Flashlight
import dev.tunderlucky.system.SystemInfo
import dev.tunderlucky.voice.VoiceManager
import dev.tunderlucky.youtube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Pending(val text: String, val action: () -> String)
data class DlItem(val id: Long, val name: String, val status: String, val progress: Float, val size: String, val done: Boolean, val failed: Boolean)

class AssistantViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app
    val prefs = Prefs(ctx)
    private val secure = SecureStore(ctx)

    val messages = mutableStateListOf<ChatMsg>()
    val activity = mutableStateListOf<String>()
    val downloads = mutableStateListOf<DlItem>()
    var status by mutableStateOf("Ready")
    var pending by mutableStateOf<Pending?>(null)
    var apiStatus by mutableStateOf("Not configured")
    var apiRequests by mutableStateOf(0)
    var axEnabled by mutableStateOf(false)
    var micGranted by mutableStateOf(false)
    var sys by mutableStateOf(SystemInfo.read(ctx))
    var lowGfx by mutableStateOf(prefs.lowGfx)
    var saveHistory by mutableStateOf(prefs.saveHistory)
    var speakReplies by mutableStateOf(prefs.speak)
    var lang by mutableStateOf(prefs.lang)
    var codeText by mutableStateOf("")
    var codeOutput by mutableStateOf("")
    var codeBusy by mutableStateOf(false)
    var hasKey by mutableStateOf(secure.hasKey())

    private var torch = false
    private var lastTarget = ""
    private var polling: Job? = null
    val voice = VoiceManager(ctx) { handleInput(it) }

    companion object {
        private const val SYSTEM = "You are TUNDER LUCKY, a fast, professional personal assistant. Reply in the user's language (English, Hindi or Hinglish). Be concise. For code use fenced code blocks. Never claim you executed or tested code."
        private const val CODE_SYSTEM = "You are a senior programming assistant (Python, Java, Kotlin, HTML/CSS/JS). Give correct, runnable code in fenced blocks with brief explanations. You cannot run code; never claim it was executed or tested."
        private val YES = setOf("yes", "confirm", "ok", "haan", "ha", "han", "हाँ", "हां")
        private val NO = setOf("no", "cancel", "nahi", "nahin", "नहीं")
        private val BLOCK = Regex("password|passcode|\\botp\\b|captcha|\\bpin\\b|cvv|card number|पासवर्ड")
        private val SENS = Regex("\\b(send|post|buy|purchase|pay|delete|remove|submit|order|checkout|publish|tweet|share|confirm)\\b")
    }

    init {
        if (prefs.saveHistory) loadHistory()
        voice.lang = lang
        refresh()
        if (cloudReady()) apiStatus = "Configured (not tested)"
    }

    fun refresh() {
        axEnabled = LuckyAccessibilityService.instance != null
        if (!axEnabled) ControlState.active = false
        micGranted = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        sys = SystemInfo.read(ctx)
    }

    fun log(s: String) {
        activity.add(0, SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) + "  " + s)
        while (activity.size > 100) activity.removeAt(activity.lastIndex)
    }

    private fun cloudReady() = prefs.endpoint.startsWith("https://") && prefs.model.isNotBlank()

    // ---------- history ----------
    private fun persist() {
        if (!saveHistory) return
        val a = JSONArray()
        messages.takeLast(100).forEach { a.put(JSONObject().put("r", it.role).put("t", it.text)) }
        prefs.history = a.toString()
    }
    private fun loadHistory() {
        try {
            val a = JSONArray(prefs.history)
            for (i in 0 until a.length()) { val o = a.getJSONObject(i); messages.add(ChatMsg(o.getString("r"), o.getString("t"))) }
        } catch (e: Exception) { }
    }
    fun clearHistory() { messages.clear(); prefs.history = "[]"; log("Conversation history cleared") }
    fun clearActivity() { activity.clear() }

    // ---------- settings ----------
    fun setLang(l: String) { lang = l; prefs.lang = l; voice.lang = l }
    fun setLowGfx(v: Boolean) { lowGfx = v; prefs.lowGfx = v }
    fun setSaveHistory(v: Boolean) { saveHistory = v; prefs.saveHistory = v; if (!v) prefs.history = "[]" else persist() }
    fun setSpeak(v: Boolean) { speakReplies = v; prefs.speak = v }
    fun saveCloud(endpoint: String, model: String, key: String) {
        prefs.endpoint = endpoint.trim(); prefs.model = model.trim()
        if (key.isNotBlank()) { secure.putApiKey(key.trim()); hasKey = true }
        apiStatus = if (cloudReady()) "Configured (not tested)" else "Endpoint must be https:// and model set"
        log("Cloud settings saved")
    }
    fun clearKey() { secure.putApiKey(""); hasKey = false; log("API key removed") }

    fun testApi() {
        if (!cloudReady()) { apiStatus = "Not configured"; return }
        apiStatus = "Testing…"
        viewModelScope.launch {
            val r = CloudClient.chat(prefs.endpoint, secure.getApiKey(), prefs.model, "Reply with OK.", listOf(CloudClient.Msg("user", "ping"))) { }
            apiRequests++
            apiStatus = r.fold({ "Connected ✓" }, { "Error: ${it.message}" })
        }
    }

    // ---------- control ----------
    fun requestControl() {
        if (!axEnabled) { Launch.start(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); status = "Enable TUNDER LUCKY in Accessibility, then come back"; return }
        pending = Pending("Allow TUNDER LUCKY to tap, scroll and type on your screen when you give a command? Password/OTP fields are never operated. You can press STOP any time.") {
            ControlState.active = true; log("Phone control ON"); "Phone control is ON."
        }
    }
    fun stopAll() {
        ControlState.active = false; ControlState.last = "Stopped"; pending = null
        voice.stopAll(); status = "Stopped"; log("STOP pressed — phone control OFF")
    }
    fun confirm(yes: Boolean) {
        val p = pending ?: return
        pending = null
        if (yes) reply(p.action()) else { log("Cancelled: ${p.text.take(50)}"); reply("Cancelled.") }
    }

    // ---------- conversation ----------
    fun reply(text: String, speak: Boolean = true) {
        messages.add(ChatMsg("assistant", text)); persist(); status = "Ready"
        if (speak && speakReplies) voice.speak(forVoice(text)) else voice.state = "Idle"
    }
    private fun forVoice(t: String) = if (t.contains("```")) "Here is the code on screen." else t.take(350)

    fun handleInput(raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) return
        messages.add(ChatMsg("user", t)); log("Command: $t"); status = "Working…"
        val local = try { runLocal(t) } catch (e: Exception) { "That action failed: ${e.message}" }
        when {
            local != null -> { log("→ $local"); reply(local) }
            cloudReady() -> askCloud()
            else -> reply("I couldn't map that to a local command. For open-ended questions, add a cloud AI endpoint, model and key in Settings.")
        }
    }

    private fun askCloud() {
        status = "Thinking…"; voice.state = "Processing"
        val hist = messages.takeLast(20).map { CloudClient.Msg(it.role, it.text) }
        messages.add(ChatMsg("assistant", "…"))
        val idx = messages.lastIndex
        apiRequests++; apiStatus = "Request in flight"
        viewModelScope.launch {
            val r = CloudClient.chat(prefs.endpoint, secure.getApiKey(), prefs.model, SYSTEM, hist) { p ->
                viewModelScope.launch { if (idx < messages.size) messages[idx] = ChatMsg("assistant", p) }
            }
            r.onSuccess { full ->
                apiStatus = "Connected ✓ (last request OK)"
                if (idx < messages.size) messages[idx] = ChatMsg("assistant", full)
                persist(); status = "Ready"
                if (speakReplies) voice.speak(forVoice(full)) else voice.state = "Idle"
            }.onFailure { e ->
                apiStatus = "Error: ${e.message}"
                if (idx < messages.size) messages[idx] = ChatMsg("assistant", "Cloud AI error: ${e.message}")
                status = "Ready"; voice.state = "Idle"
            }
        }
    }

    // ---------- local command router ----------
    private fun confirmable(desc: String, act: () -> String): String {
        pending = Pending(desc, act); return "Please confirm: $desc"
    }
    private fun ctl(block: (LuckyAccessibilityService) -> String): String {
        if (!ControlState.active) return "Phone control is OFF. Turn on the Control switch (System tab or top bar) first."
        val s = LuckyAccessibilityService.instance
            ?: return "The accessibility service isn't enabled. Enable TUNDER LUCKY in Accessibility settings."
        val r = block(s); ControlState.last = r; return r
    }
    private fun extractQuery(l: String): String? {
        val m = Regex("search(?: for)? (.+)").find(l) ?: Regex("(.+?) (?:search karo|search kro|dhundo|khojo|ढूंढो)").find(l)
        val q = m?.groupValues?.get(1) ?: return null
        return q.replace(Regex("\\b(on|in|youtube|chrome|google|par|pe|me|and|for)\\b"), " ").replace(Regex("\\s+"), " ").trim().ifEmpty { null }
    }
    private fun settingsFor(t: String): String? {
        val a = when {
            "wifi" in t || "wi-fi" in t -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" in t -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" in t || "brightness" in t -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" in t || "volume" in t -> Settings.ACTION_SOUND_SETTINGS
            "accessibility" in t -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "app" in t && "setting" in t -> Settings.ACTION_APPLICATION_SETTINGS
            "setting" in t -> Settings.ACTION_SETTINGS
            else -> return null
        }
        return if (Launch.start(ctx, Intent(a))) "Opened $t" else "Couldn't open that settings page"
    }

    private fun runLocal(raw: String): String? {
        val cased = raw.trim().trimEnd('.', '!', '?')
            .replace(Regex("^(?:hey |ok |okay )?(?:tunder|thunder)(?: lucky)?,?\\s*", RegexOption.IGNORE_CASE), "")
        val l = cased.lowercase()
        val p = pending
        if (p != null) {
            if (l in YES) { pending = null; return p.action() }
            if (l in NO) { pending = null; return "Cancelled." }
        }
        if (l == "stop" || "stop controlling" in l || "ruko" in l || "band karo" in l || "रुको" in l) { stopAll(); return "Stopped. Phone control is off." }
        if (Regex("\\b(time|samay)\\b|kitne baje|समय").containsMatchIn(l)) return "The time is " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        if (Regex("\\bdate\\b|tarikh|तारीख").containsMatchIn(l)) return "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
        if ("battery" in l) { refresh(); return "Battery is ${sys.battery}%${if (sys.charging) ", charging" else ""}." }
        if (Regex("torch|flash ?light|टॉर्च").containsMatchIn(l)) {
            torch = !Regex("\\boff\\b|band|बंद").containsMatchIn(l); return Flashlight.set(ctx, torch)
        }

        // typing (before search so "type x in the search box" isn't treated as a search)
        Regex("^(?:type|likho|write)\\s+(.+?)(?:\\s+in\\s+(?:the\\s+)?(?:search\\s+)?(?:box|bar|field))?$", RegexOption.IGNORE_CASE).find(cased)?.let { m ->
            val txt = m.groupValues[1]
            if (BLOCK.containsMatchIn(txt.lowercase())) return "I won't type passwords, OTPs or card details."
            return ctl { it.typeText(txt) }
        }
        Regex("download\\s+(https?://\\S+)", RegexOption.IGNORE_CASE).find(cased)?.let { m -> requestDownload(m.groupValues[1]); return "Confirm the download in the dialog." }

        extractQuery(l)?.let { q ->
            val yt = "youtube" in l || (lastTarget == "youtube" && "chrome" !in l && "google" !in l)
            return if (yt) { if (YouTube.search(ctx, q)) { lastTarget = "youtube"; "Searching YouTube for \"$q\"" } else "Couldn't open YouTube" }
            else { if (Chrome.search(ctx, q)) { lastTarget = "chrome"; "Searching Google for \"$q\"" } else "Couldn't open a browser" }
        }

        val openM = Regex("^(?:please )?(?:open|launch|start)\\s+(.+)$").find(l)
            ?: Regex("^(.+?)\\s+(?:kholo|khol do|kholiye|open karo|chalu karo|start karo)$").find(l)
        if (openM != null) {
            val target = openM.groupValues[1].removePrefix("my ").trim()
            if (Regex("^(?:https?://)?([a-z0-9-]+\\.)+[a-z]{2,}(?:/\\S*)?$").matches(target)) {
                val url = if (target.startsWith("http")) target else "https://$target"
                lastTarget = "chrome"
                return if (Chrome.openUrl(ctx, url)) "Opening $target" else "Couldn't open $target"
            }
            settingsFor(target)?.let { return it }
            if (target == "youtube") { lastTarget = "youtube"; return if (YouTube.open(ctx)) "Opening YouTube" else "Couldn't open YouTube" }
            if (target == "chrome") { lastTarget = "chrome"; return if (Chrome.open(ctx)) "Opening Chrome" else "Chrome isn't installed" }
            return Launch.app(ctx, target)?.let { "Opening $it" } ?: "I couldn't find an app named \"$target\"."
        }

        if ("scroll" in l || "स्क्रॉल" in l) { val up = Regex("\\bup\\b|upar|ऊपर").containsMatchIn(l); return ctl { it.scroll(!up) } }
        if (l == "back" || l.startsWith("go back") || "peeche" in l || "wapas" in l) return ctl { it.global(AccessibilityService.GLOBAL_ACTION_BACK) }
        if (l == "home" || l == "go home" || "home screen" in l) return ctl { it.global(AccessibilityService.GLOBAL_ACTION_HOME) }
        if (Regex("\\bpause\\b|\\broko\\b").containsMatchIn(l) && "video" in l || l == "pause") return ctl { it.clickAny("Pause") }
        if (l.startsWith("resume") || l == "play" || l == "play video" || l == "play the video") return ctl { it.clickAny("Play video", "Play") }
        if ("next video" in l) return ctl { it.clickAny("Next") }
        if ("first video" in l || "first result" in l) return ctl { it.openFirstVideo() }
        if (l == "enter" || l == "press enter" || l == "submit search") return ctl { it.pressEnter() }
        if ("read screen" in l || "on the screen" in l || "screen padho" in l) return ctl { it.readScreen() }
        Regex("^(?:tap|click|press|select)(?: on)?\\s+(.+)$").find(l)?.let { m ->
            val tgt = m.groupValues[1]
            if (BLOCK.containsMatchIn(tgt)) return "I don't operate password, OTP, PIN or CAPTCHA controls."
            return if (SENS.containsMatchIn(tgt)) confirmable("Tap \"$tgt\" on the screen? This may send, post, buy, delete or submit something.") { ctl { it.click(tgt) } }
            else ctl { it.click(tgt) }
        }
        return null
    }

    // ---------- downloads ----------
    fun requestDownload(url: String) {
        val u = url.trim()
        if (!(u.startsWith("https://") || u.startsWith("http://"))) { reply("Enter a direct http(s) link.", false); return }
        val warn = if (u.startsWith("http://")) " (unencrypted http!)" else ""
        pending = Pending("Download ${DownloadHelper.fileName(u)}$warn from ${Uri.parse(u).host}?") { startDownload(u) }
    }
    private fun startDownload(u: String): String = try {
        val id = DownloadHelper.enqueue(ctx, u)
        downloads.add(0, DlItem(id, DownloadHelper.fileName(u), "Queued", 0f, "", done = false, failed = false))
        log("Download started: $u"); poll(); "Download started. Saved to your Downloads folder."
    } catch (e: Exception) { "Download failed: ${e.message}" }

    private fun poll() {
        if (polling?.isActive == true) return
        polling = viewModelScope.launch {
            while (true) {
                var active = false
                for (i in downloads.indices) {
                    val d = downloads[i]
                    if (d.done || d.failed) continue
                    val info = withContext(Dispatchers.IO) { DownloadHelper.query(ctx, d.id) } ?: continue
                    val pct = if (info.total > 0) info.done.toFloat() / info.total else 0f
                    val size = if (info.total > 0) "%.1f / %.1f MB".format(info.done / 1e6, info.total / 1e6) else "size unknown"
                    downloads[i] = when (info.status) {
                        8 -> d.copy(status = "Completed", progress = 1f, size = size, done = true)
                        16 -> d.copy(status = "Failed (code ${info.reason})", failed = true, size = size)
                        4 -> { active = true; d.copy(status = "Paused", progress = pct, size = size) }
                        else -> { active = true; d.copy(status = "Downloading", progress = pct, size = size) }
                    }
                }
                if (!active) break
                delay(800)
            }
        }
    }
    fun openDownload(id: Long) { if (!DownloadHelper.open(ctx, id)) reply("No app can open that file, or it isn't finished.", false) }

    // ---------- coding ----------
    fun codeAsk(mode: String, prompt: String) {
        if (!cloudReady()) { codeOutput = "Cloud AI isn't configured. Add endpoint, model and API key in Settings → Cloud AI. (Code generation needs an AI provider.)"; return }
        val fence = "```"
        val user = when (mode) {
            "Explain" -> "Explain this code clearly. $prompt\n$fence\n$codeText\n$fence"
            "Debug" -> "Find bugs and propose fixes (you have not run the code). $prompt\n$fence\n$codeText\n$fence"
            else -> prompt + if (codeText.isNotBlank()) "\nExisting code:\n$fence\n$codeText\n$fence" else ""
        }
        codeBusy = true; codeOutput = "…"
        viewModelScope.launch {
            val r = CloudClient.chat(prefs.endpoint, secure.getApiKey(), prefs.model, CODE_SYSTEM, listOf(CloudClient.Msg("user", user))) { p ->
                viewModelScope.launch { codeOutput = p }
            }
            apiRequests++
            r.onFailure { codeOutput = "Error: ${it.message}"; apiStatus = "Error: ${it.message}" }
            codeBusy = false
        }
    }
    fun useOutputAsCode() {
        Regex("```[a-zA-Z0-9+#]*\\n([\\s\\S]*?)```").find(codeOutput)?.groupValues?.get(1)?.let { codeText = it.trimEnd() }
    }

    override fun onCleared() { voice.destroy(); super.onCleared() }
}
