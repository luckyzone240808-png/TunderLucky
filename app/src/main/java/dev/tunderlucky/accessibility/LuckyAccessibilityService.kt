package dev.tunderlucky.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** User-controlled master switch. Nothing below acts unless [active] is true. */
object ControlState {
    var active by mutableStateOf(false)
    var last by mutableStateOf("Idle")
}

class LuckyAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: LuckyAccessibilityService? = null
        private val SAFE_ENTER = setOf("com.android.chrome", "com.google.android.youtube", "com.google.android.googlequicksearchbox")
        private val RISKY_HINT = Regex("password|passcode|otp|\\bpin\\b|cvv|captcha", RegexOption.IGNORE_CASE)
    }

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; ControlState.active = false; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* intentionally passive: no screen content is stored */ }
    override fun onInterrupt() {}

    private fun walk(n: AccessibilityNodeInfo?, f: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (n == null) return null
        if (f(n)) return n
        for (i in 0 until n.childCount) { walk(n.getChild(i), f)?.let { return it } }
        return null
    }

    private fun label(n: AccessibilityNodeInfo): String =
        ((n.text?.toString() ?: "") + " " + (n.contentDescription?.toString() ?: "")).trim()

    fun foregroundPackage(): String? = rootInActiveWindow?.packageName?.toString()

    fun click(target: String): String {
        val root = rootInActiveWindow ?: return "No active window to read"
        val t = target.lowercase()
        val node = walk(root) { !it.isPassword && label(it).lowercase().contains(t) }
            ?: return "Couldn't find \"$target\" on the screen"
        var c: AccessibilityNodeInfo? = node
        while (c != null && !c.isClickable) c = c.parent
        if (c != null && c.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return "Tapped \"$target\""
        val r = Rect(); node.getBoundsInScreen(r)
        return if (tap(r.exactCenterX(), r.exactCenterY())) "Tapped \"$target\" (gesture)" else "Tap failed"
    }

    fun clickAny(vararg targets: String): String {
        for (t in targets) { val r = click(t); if (r.startsWith("Tapped")) return r }
        return "Couldn't find the control (${targets.joinToString("/")}). Tap the video once to show controls, then retry."
    }

    fun openFirstVideo(): String {
        val root = rootInActiveWindow ?: return "No active window"
        val n = walk(root) { val d = it.contentDescription?.toString() ?: ""; d.length > 25 && d.contains("views", true) }
            ?: return "Couldn't spot a video result (YouTube's layout may have changed)"
        var c: AccessibilityNodeInfo? = n
        while (c != null && !c.isClickable) c = c.parent
        return if (c?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) "Opened the first video result" else "Couldn't open it"
    }

    fun typeText(text: String): String {
        val root = rootInActiveWindow ?: return "No active window"
        val f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: walk(root) { it.isEditable && !it.isPassword }
            ?: return "No text field found. Tap a field first."
        val hint = f.hintText?.toString() ?: ""
        if (f.isPassword || RISKY_HINT.containsMatchIn(hint + " " + (f.contentDescription ?: "")))
            return "Refused: that looks like a password/OTP/PIN field. Type it yourself."
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return if (f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) "Typed \"$text\"" else "Couldn't type into that field"
    }

    fun pressEnter(): String {
        val pkg = foregroundPackage()
        if (pkg !in SAFE_ENTER) return "Not pressing Enter in this app (it could send a message). Do it yourself."
        val f = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return "No focused field"
        return if (f.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)) "Submitted" else "Couldn't submit"
    }

    fun scroll(down: Boolean): String {
        val root = rootInActiveWindow ?: return "No active window"
        val n = walk(root) { it.isScrollable }
        val ok = n?.performAction(if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true
        if (ok) return if (down) "Scrolled down" else "Scrolled up"
        val m = resources.displayMetrics
        val x = m.widthPixels / 2f
        val done = if (down) swipe(x, m.heightPixels * 0.7f, x, m.heightPixels * 0.3f)
                   else swipe(x, m.heightPixels * 0.3f, x, m.heightPixels * 0.7f)
        return if (done) "Scrolled (gesture)" else "Scroll failed"
    }

    fun global(action: Int): String = if (performGlobalAction(action)) "Done" else "Action not available"

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, dur: Long = 300): Boolean {
        val p = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val g = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, dur)).build()
        return dispatchGesture(g, null, null)
    }

    fun tap(x: Float, y: Float): Boolean {
        val p = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 60)).build()
        return dispatchGesture(g, null, null)
    }

    /** Reads visible labels aloud/on screen only; never sent anywhere. Password fields are skipped. */
    fun readScreen(): String {
        val root = rootInActiveWindow ?: return "No active window"
        val out = LinkedHashSet<String>()
        fun rec(n: AccessibilityNodeInfo?) {
            if (n == null || out.size >= 25) return
            if (!n.isPassword) { val l = label(n); if (l.isNotBlank() && l.length < 80) out.add(l) }
            for (i in 0 until n.childCount) rec(n.getChild(i))
        }
        rec(root)
        return if (out.isEmpty()) "Nothing readable on screen" else "On screen: " + out.joinToString("; ")
    }
}
