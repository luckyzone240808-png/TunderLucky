package dev.tunderlucky.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object C {
    val bg = Color(0xFF05060A)
    val cyan = Color(0xFF00E5FF)
    val blue = Color(0xFF2979FF)
    val violet = Color(0xFF9C5CFF)
    val gold = Color(0xFFD4AF37)
    val text = Color(0xFFE6F1FF)
    val dim = Color(0xFF8FA3BF)
    val red = Color(0xFFFF4D5E)
    val green = Color(0xFF3DDC97)
}

@Composable
fun TunderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = C.cyan, secondary = C.violet, background = C.bg, surface = C.bg,
            onSurface = C.text, onBackground = C.text, onPrimary = Color.Black),
        content = content
    )
}

@Composable
fun Glass(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f))))
            .border(1.dp, Brush.linearGradient(listOf(C.cyan.copy(alpha = 0.45f), C.violet.copy(alpha = 0.25f))), shape)
            .padding(12.dp),
        content = content
    )
}

@Composable
fun Pill(text: String, color: Color = C.cyan, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(text, color = color, fontSize = 13.sp,
        modifier = Modifier.clip(shape).background(color.copy(alpha = 0.12f)).border(1.dp, color.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp))
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = C.text, unfocusedTextColor = C.text, cursorColor = C.cyan,
    focusedBorderColor = C.cyan, unfocusedBorderColor = C.dim.copy(alpha = 0.5f),
    focusedLabelColor = C.cyan, unfocusedLabelColor = C.dim
)
