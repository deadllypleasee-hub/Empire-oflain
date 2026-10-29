package ru.empire.game.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.empire.core.*
import ru.empire.game.GameViewModel
import kotlin.math.roundToLong

fun LazyListScope.bankScreen(s: GameState, vm: GameViewModel) {
    item { PageTitle("Финансовый партнёр","Банк империи","Капитал любит точный расчёт.") }
    item {
        PremiumCard {
            Icon(Icons.Outlined.AccountBalance,null,tint=Gold,modifier=Modifier.size(28.dp))
            Stat("Чистый капитал",compact(Economy.netWorth(s)))
            FinanceLine("Денежные средства",s.cash)
            FinanceLine("Стоимость предприятий",s.businesses.sumOf{Economy.assetValue(it)})
            FinanceLine("В инвестициях",s.investments.sumOf{it.amount})
            FinanceLine("Долговые обязательства",-(s.loan?.remaining?:0),if(s.loan!=null)Red else Muted)
        }
    }
    item { LoanPanel(s,vm) }
    item {
        val f=Economy.finances(s)
        PremiumCard {
            Text("Структура денежного потока",style=MaterialTheme.typography.titleLarge)
            Text("ПРОГНОЗ НА ИГРОВОЙ ДЕНЬ",color=Muted,style=MaterialTheme.typography.labelSmall)
            FinanceLine("Продажи",f.revenue,Green)
            FinanceLine("Закупки и материалы",-f.supplies)
            FinanceLine("Аренда",-f.rent)
            FinanceLine("Зарплаты",-f.wages)
            FinanceLine("Обслуживание",-f.service)
            FinanceLine("УСН · 6% выручки",-f.tax)
            FinanceLine("Платёж по кредиту",-(s.loan?.installment?:0))
            HorizontalDivider(color=Border)
            val net=f.profit-(s.loan?.installment?:0)
            FinanceLine("Остаток после расходов",net,if(net>=0)Green else Red)
            Text("Выручка и затраты меняются каждый день. Сохраняйте резерв на несколько платежей.",color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
    }
    item {
        PremiumCard {
            Text("Налоговая дисциплина",style=MaterialTheme.typography.titleMedium)
            Stat("Уплачено за всё время",compact(s.totalTax),Gold)
            Text("Налог удерживается автоматически с выручки предприятий и личных заказов. При отрицательном балансе обязательства отражаются в овердрафте.",color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
    }
    if(s.history.isNotEmpty()) item {
        PremiumCard {
            Text("Последние расчёты",style=MaterialTheme.typography.titleMedium)
            s.history.takeLast(7).reversed().forEach { d -> FinanceLine("День ${d.day}",d.net,if(d.net>=0)Green else Red) }
        }
    }
}

@Composable private fun LoanPanel(s: GameState, vm: GameViewModel) {
    var fraction by rememberSaveable { mutableFloatStateOf(0f) }
    var term by rememberSaveable { mutableIntStateOf(30) }
    var confirm by remember { mutableStateOf(false) }
    var repay by remember { mutableStateOf(false) }
    val loan=s.loan
    PremiumCard {
        if(loan!=null) {
            Text("Ваш кредит",style=MaterialTheme.typography.titleLarge)
            MetricChip("АВТОМАТИЧЕСКОЕ ПОГАШЕНИЕ")
            FinanceLine("Получено",loan.principal)
            FinanceLine("Остаток к возврату",loan.remaining,Gold)
            FinanceLine("Ежедневный платёж",minOf(loan.installment,loan.remaining),Red)
            Text("Осталось ${loan.daysLeft} игровых дней. Платежи списываются автоматически, в том числе офлайн.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(progress={1-loan.daysLeft.toFloat()/loan.term},color=Gold,trackColor=Border,modifier=Modifier.fillMaxWidth())
            if(s.cash<loan.installment*3) Text("Низкий резерв: сохраните деньги на ближайшие платежи. Уход в минус снижает репутацию.",color=Red,style=MaterialTheme.typography.bodyMedium)
            GoldButton("Погасить досрочно",{repay=true},Modifier.fillMaxWidth(),s.cash>=loan.remaining)
        } else {
            val limit=Economy.loanLimit(s)
            val amount=((1000+(limit-1000)*fraction)/1000).roundToLong().coerceAtLeast(1)*1000
            val bounded=amount.coerceAtMost(limit)
            val total=Economy.money(bounded*if(term==30)1.12 else 1.28)
            Text("Кредит на развитие",style=MaterialTheme.typography.titleLarge)
            Text("Лимит ${compact(limit)} · до 60% капитала",color=Muted,style=MaterialTheme.typography.bodyMedium)
            Text(compact(bounded),color=Gold,fontSize=30.sp,fontWeight=FontWeight.SemiBold)
            Slider(value=fraction,onValueChange={fraction=it},colors=SliderDefaults.colors(thumbColor=Gold,activeTrackColor=Gold))
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                listOf(30,90).forEach { t -> FilterChip(selected=term==t,onClick={term=t},label={Text("$t дней · ${if(t==30)12 else 28}%")}) }
            }
            FinanceLine("Полная сумма возврата",total)
            FinanceLine("Ежедневный платёж",kotlin.math.ceil(total.toDouble()/term).toLong())
            Text("Процент за весь срок: ${if(term==30)12 else 28}%, включён в долг сразу. Досрочное погашение не уменьшает процент. 1 игровой день = 1 минута.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            GoldButton("Оформить кредит",{confirm=true},Modifier.fillMaxWidth(),!s.bankrupt && s.cash>=0 && s.reputation>=15)
            if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Подтвердить кредит")},text={Text("Банк перечислит ${rub(bounded)}. Вернуть ${rub(total)} за $term игровых дней. Платёж ${rub(kotlin.math.ceil(total.toDouble()/term).toLong())} каждый день, включая время вне игры.")},
                confirmButton={TextButton(onClick={vm.act{Economy.borrow(it,bounded,term)};confirm=false}){Text("Получить кредит")}},dismissButton={TextButton(onClick={confirm=false}){Text("Отмена")}})
        }
    }
    if(repay && loan!=null) AlertDialog(onDismissRequest={repay=false},title={Text("Погасить кредит?")},text={Text("Со счёта будет списано ${rub(loan.remaining)}. Долг будет полностью закрыт.")},
        confirmButton={TextButton(onClick={vm.act(Economy::repay);repay=false}){Text("Погасить")}},dismissButton={TextButton(onClick={repay=false}){Text("Отмена")}})
}
