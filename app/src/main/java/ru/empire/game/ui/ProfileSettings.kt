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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.empire.core.*
import ru.empire.game.GameViewModel

fun LazyListScope.profileScreen(s: GameState,vm: GameViewModel) {
    item { PageTitle("Личное дело","Профиль предпринимателя","Ваше имя — самый ценный актив.") }
    item {
        PremiumCard {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(76.dp).background(Gold.copy(alpha=.10f),CircleShape).border(1.dp,Gold.copy(alpha=.35f),CircleShape),contentAlignment=Alignment.Center) {
                    Icon(Icons.Outlined.WorkspacePremium,null,tint=Gold,modifier=Modifier.size(40.dp))
                }
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    val worth=Economy.netWorth(s)
                    Text(when { worth>=1_000_000_000->"Миллиардер"; worth>=1_000_000->"Инвестор";s.businesses.size>=3->"Предприниматель";else->"Основатель" },style=MaterialTheme.typography.headlineMedium)
                    Text("Уровень ${Economy.level(s)} · ${s.experience} опыта",color=Gold,style=MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(color=Border)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("Репутация в городе",color=Muted)
                Text("${s.reputation.toInt()} / 100",color=Gold,fontWeight=FontWeight.SemiBold)
            }
            LinearProgressIndicator(progress={s.reputation.toFloat()/100},color=Gold,trackColor=Border,modifier=Modifier.fillMaxWidth().height(6.dp))
            Text("Прибыльные дни укрепляют репутацию. Кассовые разрывы и непосильные кредиты снижают доверие. Новые отрасли открываются по мере роста.",color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
    }
    item {
        PremiumCard {
            Text("Путь к миллиарду",style=MaterialTheme.typography.titleLarge)
            val progress=(Economy.netWorth(s)/1_000_000_000.0).coerceIn(0.0,1.0).toFloat()
            Text("${compact(Economy.netWorth(s))} / 1 млрд ₽",color=Gold)
            LinearProgressIndicator(progress={progress},color=Gold,trackColor=Border,modifier=Modifier.fillMaxWidth().height(5.dp))
            FinanceLine("Выручка за всё время",s.totalRevenue)
            FinanceLine("Налогов уплачено",s.totalTax)
            Text("Игровых дней: ${s.day} · Заказов лично: ${s.orders}\nУлучшений: ${s.upgrades} · Восстановлений: ${s.bankruptcies}",color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
    }
    item { SectionTitle("Зал достижений","${s.achievementsClaimed.size} / ${Catalog.achievements.size} наград получено") }
    items(Catalog.achievements,key={it.id}) { a ->
        val progress=a.progress(s).coerceIn(0.0,1.0).toFloat()
        val claimed=a.id in s.achievementsClaimed
        PremiumCard {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(if(claimed)Icons.Outlined.Verified else Icons.Outlined.EmojiEvents,null,tint=if(claimed)Green else Gold,modifier=Modifier.size(28.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(a.title,style=MaterialTheme.typography.titleMedium)
                    Text(a.description,color=Muted,style=MaterialTheme.typography.bodyMedium)
                }
            }
            LinearProgressIndicator(progress={progress},color=if(claimed)Green else Gold,trackColor=Border,modifier=Modifier.fillMaxWidth().height(3.dp))
            if(progress>=1f && !claimed) GoldButton("Получить ${compact(a.reward)}",{vm.act{Economy.claimAchievement(it,a.id)}},Modifier.fillMaxWidth())
            else Text(if(claimed) "Награда получена" else "${(progress*100).toInt()}% · Награда ${compact(a.reward)}",color=if(claimed)Green else Muted,style=MaterialTheme.typography.bodyMedium)
        }
    }
}

fun LazyListScope.settingsScreen(s: GameState,vm: GameViewModel,notification: (Boolean)->Unit) {
    item { PageTitle("Ваши предпочтения","Настройки","Игра в вашем ритме.") }
    item {
        PremiumCard {
            SettingsToggle("Звуки действий","Короткий сигнал при успешной операции",Icons.Outlined.VolumeUp,s.settings.sound,vm::sound)
            HorizontalDivider(color=Border)
            SettingsToggle("Напоминания","Локальное уведомление через 8 часов после выхода",Icons.Outlined.Notifications,s.settings.notifications,notification)
        }
    }
    item {
        PremiumCard {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) { Icon(Icons.Outlined.CloudDone,null,tint=Green); Text("Прогресс на устройстве",style=MaterialTheme.typography.titleMedium) }
            Text("Каждое действие сохраняется автоматически. Для игры не нужны интернет и аккаунт. Удаление приложения удалит прогресс.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            GoldButton("Сохранить сейчас",vm::saveNow,Modifier.fillMaxWidth())
            Text("Дата сохранения: ${java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT,java.util.Locale.forLanguageTag("ru-RU")).format(java.util.Date(s.highWaterMs))}",color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
    }
    item {
        PremiumCard {
            Text("Правила времени и экономики",style=MaterialTheme.typography.titleMedium)
            Text("1 игровой день = 60 секунд. При возвращении рассчитываются максимум 8 часов. В расчёт входят расходы, налоги, кредиты и экономические события. Между перезагрузками телефона используются монотонные часы: смена системной даты не ускоряет доход. После перезагрузки более ранняя дата не даёт офлайн-дохода.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            Text("Валюта игровая: российские рубли. Все предприятия, цены и события — модель для игры. В игре нет покупок за реальные деньги и рекламы сторонних сервисов.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick={vm.tutorial(0)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(13.dp)){Text("Повторить обучение")}
        }
    }
    item {
        PremiumCard {
            Text("Новая история",style=MaterialTheme.typography.titleMedium)
            Text("Полный сброс удалит предприятия, деньги, достижения и задания. Это действие нельзя отменить.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            ResetGameButton(vm)
        }
    }
    item { Column(Modifier.fillMaxWidth().padding(15.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text("EMPIRE",color=Gold,letterSpacing=4.sp,fontSize=18.sp)
        Text("Путь миллиардера · Версия 1.0.0",color=Muted,style=MaterialTheme.typography.bodyMedium)
        Text("Ваша империя начинается с решения.",color=GoldMuted,style=MaterialTheme.typography.bodyMedium)
    } }
}
@Composable private fun SettingsToggle(title: String,description: String,icon: androidx.compose.ui.graphics.vector.ImageVector,value: Boolean,change: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Icon(icon,null,tint=Gold,modifier=Modifier.size(23.dp))
        Column(Modifier.weight(1f)) { Text(title,style=MaterialTheme.typography.titleMedium); Text(description,color=Muted,style=MaterialTheme.typography.bodyMedium) }
        Switch(checked=value,onCheckedChange=change)
    }
}
@Composable fun ResetGameButton(vm: GameViewModel) {
    var confirm by remember { mutableStateOf(false) }
    OutlinedButton(onClick={confirm=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(13.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Red)){Text("Сбросить игру")}
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Удалить весь прогресс?")},text={Text("Все деньги, предприятия, кредиты, инвестиции и достижения будут удалены. Начнётся новая игра с кофейной стойкой и 1 000 ₽.")},
        confirmButton={TextButton(onClick={vm.reset();confirm=false}){Text("Удалить и начать заново",color=Red)}},dismissButton={TextButton(onClick={confirm=false}){Text("Отмена")}})
}
