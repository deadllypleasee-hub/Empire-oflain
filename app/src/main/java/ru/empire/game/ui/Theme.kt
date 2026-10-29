package ru.empire.game.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

val Ink = Color(0xFF0C0E12)
val Panel = Color(0xFF171A20)
val PanelLight = Color(0xFF20242C)
val Gold = Color(0xFFD7B978)
val GoldMuted = Color(0xFF8B754E)
val White = Color(0xFFF3F2EF)
val Muted = Color(0xFF9299A6)
val Green = Color(0xFF7AC7A4)
val Red = Color(0xFFE88D8D)
val Border = Color(0xFF2A2E37)
private val rubles = NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU"))
fun rub(value: Long): String = "${rubles.format(value)} ₽"
fun businessCount(value: Int): String {
    val word = when {
        value % 100 in 11..14 -> "бизнесов"
        value % 10 == 1 -> "бизнес"
        value % 10 in 2..4 -> "бизнеса"
        else -> "бизнесов"
    }
    return "$value $word"
}
fun compact(value: Long): String {
    val v = kotlin.math.abs(value.toDouble())
    val sign = if(value < 0) "−" else ""
    return when {
        v >= 1_000_000_000 -> "$sign${String.format(Locale.forLanguageTag("ru-RU"), "%.2f", v / 1_000_000_000)} млрд ₽"
        v >= 1_000_000 -> "$sign${String.format(Locale.forLanguageTag("ru-RU"), "%.2f", v / 1_000_000)} млн ₽"
        else -> rub(value)
    }
}

@Composable fun EmpireTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Gold, onPrimary = Ink, primaryContainer = Color(0xFF3E3526),
            secondary = Green, background = Ink, onBackground = White, surface = Panel, onSurface = White,
            surfaceVariant = PanelLight, onSurfaceVariant = Muted, outline = Border, error = Red),
        typography = Typography(
            displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 42.sp, letterSpacing = (-1.5).sp),
            headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = (-.8).sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-.5).sp),
            titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
            bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
            bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
            labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = .8.sp),
        ), content = content)
}
