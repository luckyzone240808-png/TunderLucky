package dev.tunderlucky.ui

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Shared audio-level / activity value (0..1) read by the orb every frame. */
object OrbState { @Volatile var amp = 0f }

private const val VS = "attribute vec2 aPos; void main(){ gl_Position = vec4(aPos,0.0,1.0); }"
private const val FS = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform vec2 uRes; uniform float uTime; uniform float uAmp;
void main(){
  vec2 p = (gl_FragCoord.xy - 0.5*uRes) / min(uRes.x, uRes.y) * 2.0;
  float r = length(p);
  float R = 0.72 + 0.06*uAmp;
  vec3 bg = vec3(0.02,0.024,0.04);
  vec3 col = bg;
  if (r < R) {
    float z = sqrt(R*R - r*r);
    vec3 n = normalize(vec3(p, z));
    float c = cos(uTime*0.6), s = sin(uTime*0.6);
    vec3 q = vec3(c*n.x + s*n.z, n.y, -s*n.x + c*n.z);
    float w = (sin(q.x*6.0+uTime) + sin(q.y*7.0-uTime*1.3) + sin(q.z*5.0+uTime*0.7)) / 3.0 * 0.5 + 0.5;
    vec3 base = mix(vec3(0.05,0.15,0.65), vec3(0.0,0.9,1.0), w);
    base = mix(base, vec3(0.6,0.25,1.0), smoothstep(0.55,1.0, sin(q.y*3.0+uTime)*0.5+0.5)*0.55);
    vec3 L = normalize(vec3(-0.5,0.6,0.7));
    float diff = max(dot(n,L),0.0);
    float fres = pow(1.0 - n.z, 3.0);
    float spec = pow(max(dot(reflect(-L,n), vec3(0.0,0.0,1.0)),0.0), 24.0);
    float grid = smoothstep(0.96,1.0, max(sin(q.x*24.0), sin(q.y*24.0)))*0.25;
    col = base*(0.3+0.9*diff) + vec3(0.3,0.8,1.0)*fres*(1.0+uAmp*1.5) + vec3(1.0,0.85,0.5)*spec*0.5 + grid*vec3(0.3,0.9,1.0);
  } else {
    float g = exp(-(r-R)*5.0) * (0.35 + 0.5*uAmp);
    float ring = exp(-abs(r-(R+0.12+0.04*sin(uTime*2.0)))*40.0)*0.35;
    col = bg + vec3(0.1,0.5,1.0)*g + vec3(0.6,0.3,1.0)*ring;
  }
  gl_FragColor = vec4(col, 1.0);
}
"""

class OrbRenderer : GLSurfaceView.Renderer {
    private var prog = 0
    private var w = 1
    private var h = 1
    private val t0 = System.nanoTime()
    private val quad = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)); position(0)
    }
    private var smooth = 0f

    private fun shader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src); GLES20.glCompileShader(s)
        val ok = IntArray(1); GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        return if (ok[0] == 0) 0 else s
    }
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val v = shader(GLES20.GL_VERTEX_SHADER, VS); val f = shader(GLES20.GL_FRAGMENT_SHADER, FS)
        if (v == 0 || f == 0) { prog = 0; return }
        prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, v); GLES20.glAttachShader(prog, f); GLES20.glLinkProgram(prog)
        GLES20.glClearColor(0.02f, 0.024f, 0.04f, 1f)
    }
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) { GLES20.glViewport(0, 0, width, height); w = width; h = height }
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (prog == 0) return
        smooth += (OrbState.amp - smooth) * 0.2f
        GLES20.glUseProgram(prog)
        val pos = GLES20.glGetAttribLocation(prog, "aPos")
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 0, quad)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(prog, "uRes"), w.toFloat(), h.toFloat())
        GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uTime"), ((System.nanoTime() - t0) / 1e9).toFloat())
        GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uAmp"), smooth)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}

class OrbGLView(ctx: Context) : GLSurfaceView(ctx) {
    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        setRenderer(OrbRenderer())
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

@Composable
fun OrbView(lowGfx: Boolean, modifier: Modifier = Modifier) {
    if (lowGfx) { CanvasOrb(modifier); return }
    val act = LocalContext.current as ComponentActivity
    val view = remember { OrbGLView(act) }
    DisposableEffect(view) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) view.onPause() else if (e == Lifecycle.Event.ON_RESUME) view.onResume()
        }
        act.lifecycle.addObserver(obs)
        onDispose { act.lifecycle.removeObserver(obs); view.onPause() }
    }
    AndroidView(factory = { view }, modifier = modifier)
}

/** Low-performance fallback: cheap animated Canvas orb (no GL). */
@Composable
fun CanvasOrb(modifier: Modifier = Modifier) {
    val tr = rememberInfiniteTransition(label = "orb")
    val rot = tr.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "r")
    val pulse = tr.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "p")
    Canvas(modifier) {
        val c = center
        val base = size.minDimension * 0.34f
        val rad = base * pulse.value * (1f + OrbState.amp * 0.15f)
        drawCircle(Brush.radialGradient(listOf(Color(0x5500E5FF), Color.Transparent), c, rad * 1.9f), rad * 1.9f, c)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF7FF3FF), Color(0xFF2979FF), Color(0xFF2A0F6B)),
            Offset(c.x - rad * 0.3f, c.y - rad * 0.35f), rad * 1.25f), rad, c)
        for (i in 0..2) {
            drawArc(Brush.sweepGradient(listOf(Color.Transparent, if (i == 1) Color(0xFF9C5CFF) else Color(0xFF00E5FF), Color.Transparent), c),
                rot.value * (if (i == 1) -1.4f else 1f) + i * 120f, 160f, false,
                Offset(c.x - rad * (1.2f + i * 0.18f), c.y - rad * (1.2f + i * 0.18f)),
                Size(rad * (2.4f + i * 0.36f), rad * (2.4f + i * 0.36f)), style = Stroke(3f))
        }
    }
}

@Composable
fun Waveform(active: Boolean, modifier: Modifier = Modifier) {
    val tr = rememberInfiniteTransition(label = "wave")
    val ph = tr.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "ph")
    Canvas(modifier) {
        val n = 36
        val gap = size.width / n
        for (i in 0 until n) {
            val env = 1f - abs(i - n / 2f) / (n / 2f) * 0.6f
            val amp = if (active) 0.15f + 0.85f * OrbState.amp.coerceAtLeast(0.25f) else 0.12f
            val hgt = size.height * (0.1f + 0.9f * amp * env * (0.5f + 0.5f * sin(ph.value + i * 0.55f)))
            val x = gap * (i + 0.5f)
            drawLine(Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFF9C5CFF))),
                Offset(x, size.height / 2 - hgt / 2), Offset(x, size.height / 2 + hgt / 2), gap * 0.5f)
        }
    }
}
