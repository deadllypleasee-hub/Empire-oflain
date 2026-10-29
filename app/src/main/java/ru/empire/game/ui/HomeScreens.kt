package ru.empire.game.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.empire.core.*
import ru.empire.game.GameViewModel

fun LazyListScope.homeScreen(s: GameState, vm: GameViewModel, navigate: (String)->Unit, manage: (String)->Unit) {
    val f = Economy.finances(s)
    item {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text("ВАША ИМПЕРИЯ",color=Muted,style=MaterialTheme.typography.labelSmall)
                Text(if(s.businesses.size==1) "Всё начинается с вас." else "Масштаб ваших амбиций.",style=MaterialTheme.typography.headlineMedium)
            }
            MetricChip("ДЕНЬ ${s.day}")
        }
    }
    item {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFF302A20),Panel,androidx.compose.ui.graphics.Color(0xFF1A2023))),RoundedCornerShape(25.dp))
            .border(1.dp,Gold.copy(alpha=.24f),RoundedCornerShape(25.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text("ДОСТУПНО НА СЧЁТЕ",color=Gold,style=MaterialTheme.typography.labelSmall)
                Icon(Icons.Outlined.AccountBalanceWallet,null,tint=Gold,modifier=Modifier.size(19.dp))
            }
            Text(compact(s.cash),style=MaterialTheme.typography.displayLarge,fontSize=if(s.cash<1_000_000)42.sp else 32.sp,color=White)
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.NorthEast,null,tint=Green,modifier=Modifier.size(15.dp))
                Text("${compact(f.profit-(s.loan?.installment?:0))} / игровой день",color=if(f.profit-(s.loan?.installment?:0)>=0)Green else Red,style=MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(color=Gold.copy(alpha=.12f),modifier=Modifier.padding(vertical=7.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Капитал",compact(Economy.netWorth(s)),modifier=Modifier.weight(1f))
                Stat("Предприятия","${s.businesses.size} / 15",modifier=Modifier.weight(1f))
            }
        }
    }
    item {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            PremiumCard(Modifier.weight(1f)) { Icon(Icons.Outlined.TrendingUp,null,tint=Green,modifier=Modifier.size(20.dp)); Stat("Выручка / день",compact(f.revenue),Green) }
            PremiumCard(Modifier.weight(1f)) { Icon(Icons.Outlined.ReceiptLong,null,tint=Red,modifier=Modifier.size(20.dp)); Stat("Расходы / день",compact(f.expenses),Red) }
        }
    }
    item {
        PremiumCard {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("Динамика прибыли",style=MaterialTheme.typography.titleMedium)
                Text("${s.history.size.coerceAtMost(30)} дн.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            }
            ProfitChart(s.history)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(if(s.history.isEmpty()) "Первый расчёт через минуту" else "День ${s.history.first().day}",color=Muted,style=MaterialTheme.typography.labelSmall)
                Text("День ${s.day}",color=Muted,style=MaterialTheme.typography.labelSmall)
            }
            LinearProgressIndicator(progress={s.remainderMs / Economy.DAY_MS.toFloat()},modifier=Modifier.fillMaxWidth().height(3.dp),color=Gold,trackColor=Border)
            Text("До следующего расчёта: ${(Economy.DAY_MS-s.remainderMs)/1000} сек. · 1 минута = 1 день",color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
    }
    val event = Economy.activeEvent(s)
    if(event != null) item {
        PremiumCard {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) { Icon(Icons.Outlined.Bolt,null,tint=if(event.good)Green else Gold); Text(event.title,style=MaterialTheme.typography.titleMedium) }
            Text(event.description,color=Muted,style=MaterialTheme.typography.bodyMedium)
            MetricChip("ЕЩЁ ${s.eventUntil-s.day} ДН.",if(event.good)Green else Gold)
        }
    }
    item {
        PremiumCard {
            SectionTitle("Быстрый заказ", "Личный вклад в развитие бизнеса")
            val wait = ((Economy.ORDER_COOLDOWN_MS-(s.highWaterMs-s.lastOrderMs)).coerceAtLeast(0)+999)/1000
            Text("Обслужите клиента лично. Премия растёт вместе с оборотом, налог удерживается сразу.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            GoldButton(if(wait>0) "Следующий заказ через $wait сек." else "Выполнить заказ",{vm.act{Economy.order(it,it.highWaterMs)}},Modifier.fillMaxWidth(),wait==0L)
        }
    }
    item { SectionTitle("Ваши предприятия",action="Все",onAction={navigate("business")}) }
    items(s.businesses.take(3),key={"home_"+it.kind}) { BusinessTile(it,s){manage(it.kind)} }
    item { SectionTitle("Задания на сегодня","Обновляются в 00:00 UTC") }
    items(listOf(0,1,2),key={"quest_$it"}) { id ->
        val done = id in s.questClaimed
        val progress = Economy.questProgress(s,id).coerceIn(0.0,1.0).toFloat()
        PremiumCard {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(if(done)Icons.Outlined.TaskAlt else Icons.Outlined.RadioButtonUnchecked,null,tint=if(done)Green else Gold)
                Column(Modifier.weight(1f)) {
                    Text(listOf("Оборот 1 000 ₽","Выполните 3 заказа","Улучшите бизнес")[id],style=MaterialTheme.typography.titleMedium)
                    Text("Награда ${listOf(250,300,350)[id]} ₽",color=Gold,style=MaterialTheme.typography.bodyMedium)
                }
                if(progress>=1f && !done) TextButton(onClick={vm.act{Economy.claimQuest(it,id)}}){Text("Забрать")}
                else Text(if(done) "Готово" else "${(progress*100).toInt()}%",color=Muted,style=MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(3.dp),color=if(done)Green else Gold,trackColor=Border)
        }
    }
    item { SectionTitle("Лента империи") }
    items(s.news.take(5),key={"${it.day}_${it.title}_${it.detail}"}) { n ->
        Row(horizontalArrangement=Arrangement.spacedBy(13.dp)) {
            Box(Modifier.padding(top=6.dp).size(6.dp).background(Gold,CircleShape))
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(n.title,style=MaterialTheme.typography.titleMedium)
                Text(n.detail,color=Muted,style=MaterialTheme.typography.bodyMedium)
                Text("ДЕНЬ ${n.day}",color=GoldMuted,style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}

fun LazyListScope.businessScreen(s: GameState, manage: (String)->Unit, market: ()->Unit) {
    item { PageTitle("Портфель", "Мои бизнесы", "Управляйте активами. Создавайте ценность.") }
    item {
        PremiumCard { Row(Modifier.fillMaxWidth()) {
            Stat("Чистая прибыль / день",compact(Economy.finances(s).profit),Green,Modifier.weight(1f))
            Stat("Команда","${s.businesses.sumOf{it.staff}} чел.",modifier=Modifier.weight(1f))
        } }
    }
    items(s.businesses,key={it.kind}) { b -> BusinessTile(b,s){manage(b.kind)} }
    item {
        Column(Modifier.fillMaxWidth().border(1.dp,Gold.copy(alpha=.25f),RoundedCornerShape(20.dp)).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.AddBusiness,null,tint=Gold,modifier=Modifier.size(32.dp))
            Text("Следующая глава вашей империи",style=MaterialTheme.typography.titleMedium)
            Text("15 отраслей. От кофе до корпорации.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            GoldButton("Открыть новый бизнес",market,Modifier.fillMaxWidth())
        }
    }
}
