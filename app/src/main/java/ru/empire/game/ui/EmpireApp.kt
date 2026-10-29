package ru.empire.game.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.empire.core.*
import ru.empire.game.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EmpireApp(vm: GameViewModel) {
    val game by vm.state.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("home") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDistrict by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.notifications(it) }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it, duration = SnackbarDuration.Short) } }
    BackHandler(enabled = selected == null && screen != "home") { screen = "home" }
    EmpireTheme {
        val s = game
        if(s == null) {
            Box(Modifier.fillMaxSize().background(Ink).safeDrawingPadding(),contentAlignment=Alignment.Center) {
                Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    Text("EMPIRE",color=Gold,fontSize=32.sp,letterSpacing=6.sp)
                    if(error == null) { CircularProgressIndicator(color=Gold); Text("Открываем вашу империю…",color=Muted) }
                    else { Text(error ?: "Ошибка",color=Red); GoldButton("Повторить",{vm.load()}); ResetGameButton(vm) }
                }
            }
            return@EmpireTheme
        }
        val routes = listOf("home","business","city","market","bank")
        val labels = listOf("Обзор","Бизнесы","Город","Рынок","Банк")
        val icons = listOf(Icons.Outlined.SpaceDashboard,Icons.Outlined.Storefront,Icons.Outlined.Map,Icons.Outlined.ShoppingBag,Icons.Outlined.AccountBalance)
        Scaffold(
            containerColor=Ink,
            snackbarHost={SnackbarHost(snackbar)},
            topBar={
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=22.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("EMPIRE",fontSize=22.sp,fontWeight=FontWeight.SemiBold,letterSpacing=4.sp,color=Gold)
                        Text("ПУТЬ МИЛЛИАРДЕРА",style=MaterialTheme.typography.labelSmall,color=Muted,fontSize=8.sp,letterSpacing=1.5.sp)
                    }
                    IconButton(onClick={screen="settings"}) { Icon(Icons.Outlined.Settings,"Настройки",tint=Muted,modifier=Modifier.size(21.dp)) }
                    Box(Modifier.size(42.dp).background(Gold.copy(alpha=.1f),CircleShape).border(1.dp,Gold.copy(alpha=.45f),CircleShape).clickable {screen="profile"},contentAlignment=Alignment.Center) {
                        Text("${Economy.level(s)}",color=Gold,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                    }
                }
            },
            bottomBar={
                NavigationBar(containerColor=Ink,tonalElevation=0.dp) {
                    routes.forEachIndexed { i,r -> NavigationBarItem(selected=screen==r,onClick={screen=r},
                        icon={Icon(icons[i],labels[i],modifier=Modifier.size(22.dp))},label={Text(labels[i],fontSize=10.sp)},
                        colors=NavigationBarItemDefaults.colors(selectedIconColor=Gold,selectedTextColor=Gold,indicatorColor=Gold.copy(alpha=.09f),unselectedIconColor=Muted,unselectedTextColor=Muted)) }
                }
            }
        ) { inset ->
            AnimatedContent(targetState=screen,modifier=Modifier.padding(inset),transitionSpec={fadeIn() togetherWith fadeOut()},label="page") { page ->
                LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=10.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    if(error != null) item {
                        PremiumCard { Text(error ?: "",color=Red); GoldButton("Повторить сохранение",vm::saveNow) }
                    }
                    when(page) {
                        "home" -> homeScreen(s,vm,{screen=it},{selected=it})
                        "business" -> businessScreen(s,{selected=it},{screen="market"})
                        "city" -> cityScreen(s,selectedDistrict,{selectedDistrict=it},{screen="market"},{selected=it})
                        "market" -> marketScreen(s,vm,selectedDistrict,{selectedDistrict=it},{selected=it})
                        "bank" -> bankScreen(s,vm)
                        "profile" -> profileScreen(s,vm)
                        "settings" -> settingsScreen(s,vm) { enabled ->
                            if(enabled && Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else vm.notifications(enabled)
                        }
                    }
                }
            }
        }
        val business = s.businesses.find { it.kind == selected }
        if(business != null) BusinessSheet(business,s,vm,{selected=null})
        if(s.bankrupt) AlertDialog(onDismissRequest={},icon={Icon(Icons.Outlined.TrendingDown,null,tint=Red)},title={Text("Время начать заново")},
            text={Text("Баланс ${rub(s.cash)}. Бизнес остановлен: обязательства превысили резерв. При реструктуризации активы и инвестиции перейдут кредиторам, долг спишется. Вы получите кофейную стойку и 1 000 ₽. Достижения сохранятся.")},
            confirmButton={TextButton(onClick={vm.act(Economy::recover)}){Text("Восстановить бизнес")}})
        else if(s.tutorialStep<4) Tutorial(s.tutorialStep,{vm.tutorial(it)})
        else offline?.let { o ->
            AlertDialog(onDismissRequest=vm::dismissOffline,icon={Icon(Icons.Outlined.NightsStay,null,tint=Gold)},title={Text("С возвращением")},
                text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Text("Пока вас не было, прошло ${o.days} игровых дней.")
                    Text((if(o.earned>=0) "+" else "")+compact(o.earned),color=if(o.earned>=0)Green else Red,fontSize=28.sp,fontWeight=FontWeight.SemiBold)
                    Text("Изменение баланса с учётом расходов, налогов, кредита и выплат инвестиций.",color=Muted)
                    if(o.capped) Text("Начислено за первые 8 часов отсутствия. Остальное время пропущено.",color=Gold)
                }},confirmButton={TextButton(onClick=vm::dismissOffline){Text("К управлению")}})
        }
    }
}

@Composable private fun Tutorial(step: Int, setStep: (Int)->Unit) {
    val titles = listOf("Ваша первая империя", "Каждая минута — новый день", "Развивайте с умом", "От стойки до корпорации")
    val details = listOf(
        "У вас 1 000 ₽ и собственная кофейная стойка. Она уже продаёт кофе и приносит доход. Начните с небольших улучшений.",
        "Один игровой день длится 60 секунд. Выручка, аренда, закупки, зарплаты и налог 6% рассчитываются автоматически. Офлайн учитывается до 8 часов.",
        "Оборудование и сотрудники увеличивают выручку. Реклама работает 7 дней. Следите за чистой прибылью и платежами по кредиту — деньги на счёте важнее громкого открытия.",
        "Покупайте предприятия на рынке, осваивайте районы и выполняйте задания. Репутация открывает крупные сделки. Цель — капитал от миллиарда рублей. Всё достижимо без доната.",
    )
    AlertDialog(onDismissRequest={setStep(4)},icon={Icon(Icons.Outlined.Diamond,null,tint=Gold)},title={Text(titles[step.coerceIn(0,3)])},
        text={Column(verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(details[step.coerceIn(0,3)])
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) { repeat(4){i->Box(Modifier.size(if(i==step) 9.dp else 6.dp).background(if(i==step)Gold else Border,CircleShape))} }
        }},confirmButton={TextButton(onClick={setStep(step+1)}){Text(if(step==3) "Начать путь" else "Далее")}},
        dismissButton={TextButton(onClick={setStep(4)}){Text("Пропустить",color=Muted)}})
}
