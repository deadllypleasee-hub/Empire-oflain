package ru.empire.game.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.empire.core.*
import ru.empire.game.GameViewModel

fun LazyListScope.cityScreen(s: GameState, district: Int, choose: (Int)->Unit, market: ()->Unit, manage: (String)->Unit) {
    item { PageTitle("Территория роста","Карта города","У каждого района свой ритм. Найдите ваш.") }
    item {
        Box(Modifier.fillMaxWidth().height(260.dp).background(Panel,RoundedCornerShape(24.dp)).border(1.dp,Border,RoundedCornerShape(24.dp))) {
            Canvas(Modifier.fillMaxSize().padding(15.dp)) {
                val w = size.width; val h = size.height
                val river = Path().apply { moveTo(w*.63f,0f); cubicTo(w*.36f,h*.35f,w*.86f,h*.58f,w*.62f,h); lineTo(w*.72f,h); cubicTo(w*.96f,h*.58f,w*.46f,h*.35f,w*.73f,0f); close() }
                drawPath(river,Color(0xFF24343C))
                for(row in 0..4) for(col in 0..5) {
                    val x = col*w*.17f; val y = row*h*.22f
                    if(col!=3) drawRoundRect(Color(0xFF222830),Offset(x,y),Size(w*.12f,h*.15f),androidx.compose.ui.geometry.CornerRadius(5f,5f))
                    if((row+col)%3==0 && col!=3) drawRect(Gold.copy(alpha=.10f),Offset(x+w*.02f,y+h*.025f),Size(w*.075f,h*.09f))
                }
                drawLine(Gold.copy(alpha=.18f),Offset(0f,h*.5f),Offset(w,h*.5f),5f)
            }
            val positions = listOf(Alignment.TopStart,Alignment.BottomStart,Alignment.Center,Alignment.TopEnd)
            Catalog.districts.forEachIndexed { i,d ->
                Column(Modifier.align(positions[i]).padding(if(i==2) 12.dp else 20.dp)
                    .background(if(i==district)Gold else Ink.copy(alpha=.9f),RoundedCornerShape(11.dp))
                    .border(1.dp,if(i==district)Gold else Border,RoundedCornerShape(11.dp)).clickable{choose(i)}.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(if(s.reputation>=d.reputation)Icons.Outlined.LocationOn else Icons.Outlined.Lock,null,tint=if(i==district)Ink else Gold,modifier=Modifier.size(18.dp))
                    Text(d.name,fontSize=10.sp,color=if(i==district)Ink else White)
                    Text("${s.businesses.count{it.district==i}} бизнесов",fontSize=9.sp,color=if(i==district)Ink.copy(alpha=.65f) else Muted)
                }
            }
        }
    }
    items(Catalog.districts.indices.toList(),key={"district_$it"}) { i ->
        val d = Catalog.districts[i]
        val open = s.reputation >= d.reputation
        PremiumCard(Modifier.clickable {choose(i)}) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Icon(if(open)Icons.Outlined.LocationCity else Icons.Outlined.Lock,null,tint=if(district==i)Gold else Muted)
                Column(Modifier.weight(1f).padding(start=12.dp)) {
                    Text(d.name,style=MaterialTheme.typography.titleMedium,color=if(district==i)Gold else White)
                    Text(d.description,color=Muted,style=MaterialTheme.typography.bodyMedium)
                }
                if(district==i) Icon(Icons.Outlined.CheckCircle,null,tint=Gold,modifier=Modifier.size(20.dp))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                MetricChip("СПРОС ×${d.salesFactor}",Green)
                MetricChip("АРЕНДА ×${d.rentFactor}",Muted)
            }
            if(!open) Text("Откроется при репутации ${d.reputation} · сейчас ${s.reputation.toInt()}",color=Gold,style=MaterialTheme.typography.bodyMedium)
            else if(i==district) GoldButton("Найти бизнес в районе",market,Modifier.fillMaxWidth())
        }
    }
    item { SectionTitle("В выбранном районе",Catalog.districts[district].name) }
    val owned = s.businesses.filter{it.district==district}
    if(owned.isEmpty()) item { Text("Здесь ещё нет ваших предприятий. Выберите площадку на рынке или перенесите действующий бизнес через управление.",color=Muted,style=MaterialTheme.typography.bodyLarge) }
    items(owned,key={"city_"+it.kind}) { b -> BusinessTile(b,s){manage(b.kind)} }
}

fun LazyListScope.marketScreen(s: GameState, vm: GameViewModel, district: Int, choose: (Int)->Unit, manage: (String)->Unit) {
    item { PageTitle("Новые возможности","Рынок","Следующая инвестиция в ваше будущее.") }
    item { MarketControls(s,vm,district,choose,manage) }
}

@Composable private fun MarketControls(s: GameState, vm: GameViewModel, district: Int, choose: (Int)->Unit, manage: (String)->Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var purchase by remember { mutableStateOf<BusinessType?>(null) }
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Предприятия","Инвестиции").forEachIndexed{i,t->SegmentedButton(selected=tab==i,onClick={tab=i},shape=SegmentedButtonDefaults.itemShape(i,2)){Text(t)}}
        }
        if(tab==0) {
            Text("РАЙОН ОТКРЫТИЯ",color=Muted,style=MaterialTheme.typography.labelSmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Catalog.districts.forEachIndexed { i,d -> FilterChip(selected=district==i,onClick={choose(i)},label={Text(d.name)},leadingIcon=if(s.reputation<d.reputation){{Icon(Icons.Outlined.Lock,null,modifier=Modifier.size(13.dp))}} else null) }
            }
            Catalog.businesses.forEach { t ->
                val owned = s.businesses.any{it.kind==t.id}
                val locked = s.reputation < maxOf(t.reputation,Catalog.districts[district].reputation)
                val price = Economy.price(t.id,district)
                val estimate = Economy.finance(Business(t.id,district),s)
                PremiumCard {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(15.dp)) {
                        BusinessArt(t,Modifier.size(92.dp),true)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(t.name,style=MaterialTheme.typography.titleMedium)
                            Text(t.subtitle,color=Muted,style=MaterialTheme.typography.bodyMedium)
                            Text(if(owned) "В ВАШЕЙ ИМПЕРИИ" else if(locked) "РЕПУТАЦИЯ ${maxOf(t.reputation,Catalog.districts[district].reputation)}" else "ДОСТУПНО К ОТКРЫТИЮ",color=if(owned)Green else Gold,style=MaterialTheme.typography.labelSmall)
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Stat("Стоимость",compact(price),modifier=Modifier.weight(1f))
                        Stat("Прибыль / день","+${compact(estimate.profit)}",Green,Modifier.weight(1f))
                    }
                    if(owned) OutlinedButton(onClick={manage(t.id)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)){Text("Управлять")}
                    else GoldButton(if(locked) "Требуется репутация" else if(s.cash<price) "Нужно ещё ${compact(price-s.cash)}" else "Открыть предприятие",{purchase=t},Modifier.fillMaxWidth(),!locked && s.cash>=price && !s.bankrupt)
                }
            }
        } else InvestmentPanel(s,vm)
    }
    purchase?.let { t ->
        val f=Economy.finance(Business(t.id,district),s)
        AlertDialog(onDismissRequest={purchase=null},title={Text("Открыть ${t.name.lowercase()}?")},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text("${Catalog.districts[district].name}. Выручка меняется со спросом, популярностью и событиями.")
                FinanceLine("Стоимость",Economy.price(t.id,district),Gold)
                FinanceLine("Выручка / день",f.revenue,Green)
                FinanceLine("Все расходы / день",f.expenses,Red)
                FinanceLine("Чистая прибыль / день",f.profit,Green)
            }},confirmButton={TextButton(onClick={vm.act{Economy.buy(it,t.id,district)};purchase=null}){Text("Подтвердить покупку")}},dismissButton={TextButton(onClick={purchase=null}){Text("Отмена")}})
    }
}

@Composable private fun InvestmentPanel(s: GameState, vm: GameViewModel) {
    var amount by rememberSaveable { mutableStateOf("1000") }
    var confirm by remember { mutableStateOf<Int?>(null) }
    val names = listOf("Городские облигации","Коммерческая недвижимость","Венчурный фонд")
    val descriptions = listOf("Фиксированная выплата +8% через 30 игровых дней.","Через 45 дней: +30% с вероятностью 75%, иначе −15%.","Через 60 дней: +70% с вероятностью 60%, иначе −40%.")
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        PremiumCard {
            Text("Капитал должен работать",style=MaterialTheme.typography.titleLarge)
            Text("Средства блокируются до окончания срока. Максимум 3 одновременные инвестиции. Выплаты уже включают комиссии и налоги.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value=amount,onValueChange={v->if(v.length<=12 && v.all{it.isDigit()})amount=v},label={Text("Сумма в рублях")},singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp))
            Text("От 1 000 до 100 000 000 ₽. На счёте ${compact(s.cash)}",color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
        names.forEachIndexed { i,name -> PremiumCard {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Icon(listOf(Icons.Outlined.AccountBalance,Icons.Outlined.Apartment,Icons.Outlined.RocketLaunch)[i],null,tint=Gold)
                Text(name,style=MaterialTheme.typography.titleMedium)
            }
            Text(descriptions[i],color=Muted,style=MaterialTheme.typography.bodyMedium)
            val n=amount.toLongOrNull()?:0
            GoldButton("Инвестировать",{confirm=i},Modifier.fillMaxWidth(),n in 1_000..100_000_000 && s.cash>=n && s.investments.size<3)
        } }
        if(s.investments.isNotEmpty()) {
            SectionTitle("Активные вложения")
            s.investments.forEach { inv -> PremiumCard {
                Text(names[inv.kind],style=MaterialTheme.typography.titleMedium)
                FinanceLine("Вложено",inv.amount)
                Text("Выплата через ${inv.dueDay-s.day} игровых дней",color=Gold,style=MaterialTheme.typography.bodyMedium)
            } }
        }
    }
    confirm?.let { kind -> AlertDialog(onDismissRequest={confirm=null},title={Text("Разместить инвестицию?")},
        text={Text("${names[kind]} · ${rub(amount.toLongOrNull()?:0)}. ${descriptions[kind]} Досрочное изъятие недоступно.")},
        confirmButton={TextButton(onClick={vm.act{Economy.invest(it,kind,amount.toLongOrNull()?:0)};confirm=null}){Text("Разместить")}},dismissButton={TextButton(onClick={confirm=null}){Text("Отмена")}}) }
}
