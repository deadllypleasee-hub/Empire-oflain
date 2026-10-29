package ru.empire.game.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.empire.core.*
import ru.empire.game.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BusinessSheet(b: Business, s: GameState, vm: GameViewModel, close: ()->Unit) {
    val t = Catalog.type(b.kind)
    val f = Economy.finance(b,s)
    var sellConfirm by remember { mutableStateOf(false) }
    var moveConfirm by remember { mutableStateOf<Int?>(null) }
    ModalBottomSheet(onDismissRequest=close,containerColor=Ink,contentColor=White,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=22.dp).padding(bottom=36.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                BusinessArt(t,Modifier.size(100.dp),true)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    MetricChip("УРОВЕНЬ ${b.level} / 8")
                    Text(t.name,style=MaterialTheme.typography.headlineMedium)
                    Text(Catalog.districts[b.district].name,color=Muted,style=MaterialTheme.typography.bodyMedium)
                }
            }
            PremiumCard {
                Text("Экономика дня",style=MaterialTheme.typography.titleMedium)
                FinanceLine("Выручка",f.revenue,Green)
                FinanceLine("Закупки",-f.supplies)
                FinanceLine("Аренда",-f.rent)
                FinanceLine("Зарплаты · ${b.staff} чел.",-f.wages)
                FinanceLine("Обслуживание",-f.service)
                FinanceLine("Налог · 6% с выручки",-f.tax)
                HorizontalDivider(color=Border)
                FinanceLine("Чистая прибыль",f.profit,if(f.profit>=0)Green else Red)
                Text("Доход зависит от района, репутации, популярности и конкуренции. Сейчас влияние конкурентов: ${((Economy.competitor(b,s.day)-1)*100).toInt()}%.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            }
            PremiumCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("Популярность",style=MaterialTheme.typography.titleMedium)
                    Text("${b.popularity.toInt()} / 100",color=Gold)
                }
                LinearProgressIndicator(progress={b.popularity.toFloat()/100},color=Gold,trackColor=Border,modifier=Modifier.fillMaxWidth().height(5.dp))
                Text("Качественный сервис растит популярность каждый день. Реклама ускоряет рост.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            }
            SectionTitle("Развитие предприятия")
            ImprovementRow("Расширение", "Выручка +68% от базовой за уровень; аренда +20%",Icons.Outlined.OpenInFull,
                Economy.upgradeCost(b),b.level>=8,s.cash,{vm.act{Economy.improve(it,b.kind,"level")}})
            ImprovementRow("Оборудование · ${b.equipment}/5", "Каждая ступень даёт +20% к множителю выручки",Icons.Outlined.CoffeeMaker,
                Economy.equipmentCost(b),b.equipment>=5,s.cash,{vm.act{Economy.improve(it,b.kind,"equipment")}})
            PremiumCard {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Groups,null,tint=Gold)
                    Column { Text("Команда · ${b.staff}/${Economy.staffLimit(b)}",style=MaterialTheme.typography.titleMedium); Text("+13% к выручке за сотрудника",color=Muted,style=MaterialTheme.typography.bodyMedium) }
                }
                Text("Зарплата за сотрудника: ${rub(Economy.money(t.wage*(1+(b.level-1)*.16)))} / день. При увольнении выплачиваются 2 базовые ставки.",color=Muted,style=MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick={vm.act{Economy.improve(it,b.kind,"fire")}},enabled=b.staff>0 && s.cash>=t.wage*2,modifier=Modifier.weight(1f),shape=RoundedCornerShape(12.dp)){Text("Уволить")}
                    GoldButton("Нанять · ${compact(Economy.hireCost(b))}",{vm.act{Economy.improve(it,b.kind,"hire")}},Modifier.weight(1f),b.staff<Economy.staffLimit(b) && s.cash>=Economy.hireCost(b))
                }
            }
            ImprovementRow("Рекламная кампания",if(b.advertisingUntil>s.day) "Действует ещё ${b.advertisingUntil-s.day} дн." else "Выручка +35% на 7 дней и рост популярности",Icons.Outlined.Campaign,
                Economy.adCost(b),b.advertisingUntil>s.day,s.cash,{vm.act{Economy.improve(it,b.kind,"ad")}},if(b.advertisingUntil>s.day) "Кампания активна" else null)
            PremiumCard {
                Text("Сменить локацию",style=MaterialTheme.typography.titleMedium)
                Text("Переезд стоит 18% цены предприятия в новом районе. Оборудование и команда сохраняются.",color=Muted,style=MaterialTheme.typography.bodyMedium)
                Catalog.districts.forEachIndexed { i,d ->
                    if(i!=b.district) {
                        val cost=Economy.money(Economy.price(b.kind,i)*.18)
                        OutlinedButton(onClick={moveConfirm=i},enabled=s.reputation>=d.reputation && s.cash>=cost,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)) {
                            Text("${d.name} · ${compact(cost)}",fontSize=12.sp)
                        }
                    }
                }
            }
            Text("Прибыль за всё время: ${compact(b.lifetimeProfit)}",color=Muted,style=MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick={sellConfirm=true},enabled=s.businesses.size>1,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Red)) {
                Text("Продать за ${compact(Economy.money(Economy.assetValue(b)*.55))}")
            }
            if(s.businesses.size==1) Text("Последнее предприятие — основа вашей империи. Его продажа недоступна.",color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
    }
    if(sellConfirm) AlertDialog(onDismissRequest={sellConfirm=false},title={Text("Продать предприятие?")},text={Text("${t.name} покинет вашу империю. Вы получите 55% оценочной стоимости: ${rub(Economy.money(Economy.assetValue(b)*.55))}. Обратная покупка возможна на рынке.")},
        confirmButton={TextButton(onClick={vm.act{Economy.sell(it,b.kind)};sellConfirm=false;close()}){Text("Продать",color=Red)}},dismissButton={TextButton(onClick={sellConfirm=false}){Text("Отмена")}})
    moveConfirm?.let { i -> AlertDialog(onDismissRequest={moveConfirm=null},title={Text("Перенести бизнес?")},text={Text("${Catalog.districts[i].name}. Цена: ${rub(Economy.money(Economy.price(b.kind,i)*.18))}. Аренда ×${Catalog.districts[i].rentFactor}, спрос ×${Catalog.districts[i].salesFactor}.")},
        confirmButton={TextButton(onClick={vm.act{Economy.relocate(it,b.kind,i)};moveConfirm=null}){Text("Переехать")}},dismissButton={TextButton(onClick={moveConfirm=null}){Text("Отмена")}}) }
}

@Composable private fun ImprovementRow(title: String, text: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    cost: Long, maxed: Boolean, cash: Long, action: ()->Unit, maxLabel: String?=null) {
    PremiumCard {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(icon,null,tint=Gold,modifier=Modifier.size(25.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(title,style=MaterialTheme.typography.titleMedium); Text(text,color=Muted,style=MaterialTheme.typography.bodyMedium) }
        }
        GoldButton(if(maxed) maxLabel?:"Максимальный уровень" else "Развить · ${compact(cost)}",action,Modifier.fillMaxWidth(),!maxed && cash>=cost)
    }
}
