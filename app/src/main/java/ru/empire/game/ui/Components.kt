package ru.empire.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.empire.core.*
import kotlin.math.*

fun businessIcon(name: String): ImageVector = when(name) {
    "coffee" -> Icons.Outlined.LocalCafe
    "store" -> Icons.Outlined.Storefront
    "cart" -> Icons.Outlined.ShoppingCart
    "car" -> Icons.Outlined.DirectionsCar
    "cut" -> Icons.Outlined.ContentCut
    "food" -> Icons.Outlined.Restaurant
    "build" -> Icons.Outlined.Build
    "fitness" -> Icons.Outlined.FitnessCenter
    "hotel" -> Icons.Outlined.Hotel
    "mall" -> Icons.Outlined.Apartment
    "factory" -> Icons.Outlined.Factory
    else -> Icons.Outlined.Business
}

@Composable fun PremiumCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(Panel, RoundedCornerShape(22.dp)).border(1.dp, Border.copy(alpha=.65f), RoundedCornerShape(22.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable fun SectionTitle(title: String, caption: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if(caption != null) Text(caption, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        if(action != null) TextButton(onClick = onAction) { Text(action, color = Gold) }
    }
}
@Composable fun PageTitle(eyebrow: String, title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(eyebrow.uppercase(), color = Gold, style = MaterialTheme.typography.labelSmall)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(detail, color = Muted, style = MaterialTheme.typography.bodyLarge)
    }
}
@Composable fun Stat(label: String, value: String, color: Color = White, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = color, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable fun FinanceLine(label: String, value: Long, color: Color = White) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(compact(value), color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
@Composable fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min=48.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) { Text(text, fontWeight = FontWeight.SemiBold) }
}

/** Locally drawn illustration: no remote assets or network dependency. */
@Composable fun BusinessArt(type: BusinessType, modifier: Modifier = Modifier, large: Boolean = false) {
    val tint = Color(type.color)
    Box(modifier.background(Brush.linearGradient(listOf(tint.copy(alpha=.15f), Panel)), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.width; val h = size.height
            val isTower = type.price >= 12_000_000
            val left = w * .22f; val right = w * .78f; val top = h * if(isTower) .12f else .38f
            val base = h * .82f
            drawOval(Color.Black.copy(alpha=.23f), Offset(w*.12f, h*.81f), Size(w*.76f,h*.11f))
            val side = Path().apply { moveTo(right, top); lineTo(w*.91f, top+h*.09f); lineTo(w*.91f,base-h*.03f); lineTo(right,base); close() }
            drawPath(side, tint.copy(alpha=.32f))
            drawRoundRect(Color(0xFF333944), Offset(left,top), Size(right-left,base-top), CornerRadius(3f,3f))
            if(isTower) {
                val rows = if(type.price >= 750_000_000) 6 else 4
                for(row in 0 until rows) for(col in 0..2) {
                    drawRoundRect(if((row+col)%3==0) tint else tint.copy(alpha=.3f), Offset(left+w*.06f+col*w*.15f, top+h*.055f+row*(base-top)*.13f), Size(w*.08f, (base-top)*.06f), CornerRadius(1f,1f))
                }
                drawRect(tint, Offset(left, top-h*.035f), Size(right-left, h*.025f))
            } else {
                drawRect(tint, Offset(w*.17f, top-h*.07f), Size(w*.66f,h*.09f))
                for(i in 0..6) drawRect(if(i%2==0) tint.copy(alpha=.8f) else Color(0xFF343A43),Offset(w*.17f+i*w*.094f,top+h*.02f),Size(w*.094f,h*.085f))
                drawRoundRect(Color(0xFF162027),Offset(left+w*.04f, top+h*.16f),Size(w*.27f,h*.21f),CornerRadius(2f,2f))
                drawLine(tint.copy(alpha=.45f),Offset(left+w*.17f,top+h*.16f),Offset(left+w*.17f,top+h*.37f),1.5f)
                drawRect(tint.copy(alpha=.45f),Offset(right-w*.17f,top+h*.16f),Size(w*.11f,h*.28f))
            }
            drawLine(tint.copy(alpha=.5f),Offset(w*.13f,base+h*.025f),Offset(w*.92f,base+h*.025f),2f)
            drawCircle(tint.copy(alpha=.45f),w*.018f,Offset(w*.12f,h*.27f))
            drawLine(tint.copy(alpha=.25f),Offset(w*.87f,h*.19f),Offset(w*.94f,h*.19f),2f)
        }
        if(large) Icon(businessIcon(type.icon), null, tint = tint.copy(alpha=.85f), modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(18.dp))
    }
}

@Composable fun ProfitChart(history: List<DayReport>, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(if(history.isNotEmpty()) 1f else 0f, tween(600), label = "chart")
    Canvas(modifier.fillMaxWidth().height(115.dp)) {
        val w = size.width; val h = size.height
        repeat(3) { i -> drawLine(Border,Offset(0f,h*i/2),Offset(w,h*i/2),1f) }
        if(history.isEmpty()) {
            drawLine(Gold.copy(alpha=.5f),Offset(0f,h*.75f),Offset(w,h*.75f),2f)
            return@Canvas
        }
        val values = history.map { it.net.toFloat() }
        val low = min(0f, values.minOrNull() ?: 0f)
        val high = max(1f, values.maxOrNull() ?: 1f)
        val points = if(values.size == 1) listOf(values.first(), values.first()) else values
        val path = Path()
        points.forEachIndexed { i, value ->
            val x = i.toFloat() / (points.size - 1) * w
            val y = h*.88f - ((value - low)/(high - low)) * h*.76f * fraction
            if(i == 0) path.moveTo(x,y) else path.lineTo(x,y)
        }
        val fill = Path().apply { addPath(path); lineTo(w,h); lineTo(0f,h); close() }
        drawPath(fill, Brush.verticalGradient(listOf(Green.copy(alpha=.19f),Color.Transparent)))
        drawPath(path, Green, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
@Composable fun BusinessTile(b: Business, s: GameState, onClick: () -> Unit) {
    val t = Catalog.type(b.kind)
    val f = Economy.finance(b,s)
    Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(19.dp)).border(1.dp,Border.copy(alpha=.65f),RoundedCornerShape(19.dp))
        .clickable(onClick=onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        BusinessArt(t, Modifier.size(78.dp))
        Column(Modifier.weight(1f).padding(start=13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(t.name, style = MaterialTheme.typography.titleMedium)
            Text("${Catalog.districts[b.district].name} · Ур. ${b.level}",color=Muted,style=MaterialTheme.typography.bodyMedium)
            Text("+${compact(f.profit)} / день",color=if(f.profit>=0) Green else Red,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
        }
        Icon(Icons.Outlined.ChevronRight,null,tint=Muted,modifier=Modifier.size(20.dp))
    }
}
@Composable fun MetricChip(text: String, color: Color = Gold) {
    Text(text,color=color,style=MaterialTheme.typography.labelSmall,
        modifier=Modifier.background(color.copy(alpha=.10f),RoundedCornerShape(8.dp)).padding(horizontal=9.dp,vertical=6.dp))
}
