package ru.empire.core

import kotlin.math.*

/** All money is whole rubles; only bounded multipliers use floating point. */
object Economy {
    const val DAY_MS = 60_000L
    const val OFFLINE_CAP_MS = 8 * 60 * 60 * 1000L
    const val MAX_MONEY = 1_000_000_000_000_000L
    const val ORDER_COOLDOWN_MS = 10_000L
    fun money(value: Double) = value.roundToLong().coerceIn(0, MAX_MONEY)
    fun initial(now: Long) = GameState(highWaterMs = now.coerceAtLeast(0), questEpoch = now.coerceAtLeast(0) / 86_400_000, seed = now xor 0x5DEECE66DL)
    fun level(s: GameState) = (1 + sqrt(s.experience / 100.0).toInt()).coerceAtMost(100)
    fun activeEvent(s: GameState): EconomyEvent? = Catalog.events.getOrNull(s.eventId)?.takeIf { s.eventUntil > s.day }
    fun price(kind: String, district: Int) = money(Catalog.type(kind).price * Catalog.districts[district].priceFactor)
    fun upgradeCost(b: Business) = money(Catalog.type(b.kind).price * .24 * b.level.toDouble().pow(1.7))
    fun equipmentCost(b: Business) = money(Catalog.type(b.kind).price * .19 * (b.equipment + 1).toDouble().pow(1.6))
    fun hireCost(b: Business) = money(Catalog.type(b.kind).dailyRevenue * .14 * (b.staff + 1))
    fun adCost(b: Business) = money(Catalog.type(b.kind).dailyRevenue * .80 * (1 + (b.level - 1) * .55))
    fun staffLimit(b: Business) = 2 + b.level
    fun assetValue(b: Business) = money(price(b.kind, b.district) * (1 + (b.level - 1) * .32 + b.equipment * .18))
    fun netWorth(s: GameState) = (s.cash + s.businesses.sumOf(::assetValue) + s.investments.sumOf { it.amount } - (s.loan?.remaining ?: 0)).coerceIn(-MAX_MONEY, MAX_MONEY)
    fun competitor(b: Business, day: Long): Double {
        val hash = (b.kind.hashCode().toLong() * 31 + day / 5 * 17).let { Math.floorMod(it, 21L) }
        return .90 + hash / 100.0
    }
    fun finance(b: Business, s: GameState, closingDay: Boolean = false): Finance {
        val t = Catalog.type(b.kind)
        val d = Catalog.districts[b.district]
        val event = activeEvent(s)
        val scale = 1 + (b.level - 1) * .68
        val popularity = .82 + b.popularity.coerceIn(0.0, 100.0) / 250.0
        val revenue = money(t.dailyRevenue * scale * (1 + b.equipment * .20) * (1 + b.staff * .13) * popularity *
            d.salesFactor * competitor(b, s.day) * (event?.sales ?: 1.0) *
            (if (b.advertisingUntil > s.day || (closingDay && b.advertisingUntil == s.day)) 1.35 else 1.0) * (.94 + s.reputation / 500.0))
        return Finance(
            revenue,
            money(t.rent * d.rentFactor * (1 + (b.level - 1) * .20)),
            money(t.wage * b.staff * (1 + (b.level - 1) * .16)),
            money(t.service * scale * (1 + b.equipment * .12) * (event?.costs ?: 1.0)),
            money(revenue * t.supplyRate * (event?.costs ?: 1.0)),
            money(revenue * .06),
        )
    }
    fun finances(s: GameState, closingDay: Boolean = false): Finance {
        val all = s.businesses.map { finance(it, s, closingDay) }
        return Finance(all.sumOf { it.revenue }, all.sumOf { it.rent }, all.sumOf { it.wages }, all.sumOf { it.service }, all.sumOf { it.supplies }, all.sumOf { it.tax })
    }
    fun addNews(s: GameState, title: String, detail: String) = s.copy(news = (listOf(News(title, detail, s.day)) + s.news).take(25))

    /** Persistent high-water clock: rollback never earns money. Discard excess, keep fraction.
     * Processing a timestamp twice is idempotent, including after process death. */
    fun advance(source: GameState, nowMs: Long): AdvanceResult {
        val now = nowMs.coerceAtLeast(0)
        if (now <= source.highWaterMs) return AdvanceResult(source, 0, 0)
        val elapsed = now - source.highWaterMs
        val accepted = elapsed.coerceAtMost(OFFLINE_CAP_MS)
        val total = source.remainderMs.coerceIn(0, DAY_MS - 1) + accepted
        val days = (total / DAY_MS).toInt()
        var s = source.copy(highWaterMs = now, remainderMs = total % DAY_MS)
        // A daily reward is a real UTC day; high-water protects against repeated resets.
        val epoch = now / 86_400_000
        if (epoch > s.questEpoch) s = s.copy(questEpoch = epoch, questStartRevenue = s.totalRevenue,
            questStartOrders = s.orders, questStartUpgrades = s.upgrades, questClaimed = emptySet())
        val before = s.cash
        repeat(days) { if (!s.bankrupt) s = day(s) }
        return AdvanceResult(s, days, s.cash - before, elapsed > OFFLINE_CAP_MS)
    }

    private fun day(source: GameState): GameState {
        var s = source.copy(day = source.day + 1)
        if (s.day % 7L == 0L) {
            val idx = kotlin.random.Random(s.seed xor (s.day * 1_103_515_245L)).nextInt(Catalog.events.size)
            s = addNews(s.copy(eventId = idx, eventUntil = s.day + 3), Catalog.events[idx].title, Catalog.events[idx].description)
        }
        val f = finances(s, closingDay = true)
        var loan = s.loan
        var payment = 0L
        var rep = s.reputation
        if (loan != null) {
            payment = min(loan.installment, loan.remaining)
            val remaining = loan.remaining - payment
            val left = loan.daysLeft - 1
            if (s.cash + f.profit < payment) rep -= 1.2
            loan = if (remaining <= 0) null else loan.copy(remaining = remaining, daysLeft = left)
            if (loan == null) s = addNews(s, "Кредит погашен", "Все обязательства перед банком исполнены.")
        }
        val matured = s.investments.filter { it.dueDay <= s.day }
        val payout = matured.sumOf { it.payout }
        if (matured.isNotEmpty()) s = addNews(s, "Инвестиции завершены", "На счёт поступило $payout ₽.")
        val cash = (s.cash + f.profit - payment + payout).coerceIn(-MAX_MONEY, MAX_MONEY)
        val report = DayReport(s.day, f.revenue, f.rent, f.wages, f.service, f.supplies, f.tax, payment, f.profit - payment)
        rep += if (cash >= 0) .075 else -.4
        val bankruptcy = cash < -max(5_000L, f.revenue * 3)
        s = s.copy(cash = cash, loan = loan, reputation = rep.coerceIn(0.0, 100.0),
            totalRevenue = (s.totalRevenue + f.revenue).coerceAtMost(MAX_MONEY),
            totalTax = (s.totalTax + f.tax).coerceAtMost(MAX_MONEY),
            experience = (s.experience + 8 + s.businesses.size * 2).coerceAtMost(1_000_000),
            investments = s.investments.filter { it.dueDay > s.day },
            businesses = s.businesses.map { b -> b.copy(popularity = (b.popularity + .18 + b.staff * .025 +
                (if (b.advertisingUntil >= s.day) 1.1 else 0.0)).coerceAtMost(100.0), lifetimeProfit = (b.lifetimeProfit + finance(b, s, closingDay = true).profit).coerceIn(-MAX_MONEY, MAX_MONEY)) },
            history = (s.history + report).takeLast(30), bankrupt = bankruptcy)
        if (bankruptcy) s = addNews(s, "Финансовая несостоятельность", "Предприятия остановлены. Пройдите реструктуризацию, чтобы начать снова.")
        return s
    }

    private fun reject(s: GameState, text: String) = ActionResult(s, text, false)
    private fun spend(s: GameState, amount: Long, message: String, update: (GameState) -> GameState): ActionResult {
        if (s.bankrupt) return reject(s, "Сначала восстановите бизнес")
        if (amount < 0 || s.cash < amount) return reject(s, "Недостаточно средств")
        return ActionResult(update(s.copy(cash = s.cash - amount)), message)
    }
    fun buy(s: GameState, kind: String, district: Int): ActionResult {
        val t = Catalog.businesses.find { it.id == kind } ?: return reject(s, "Предприятие не найдено")
        val d = Catalog.districts.getOrNull(district) ?: return reject(s, "Район не найден")
        if (s.businesses.any { it.kind == kind }) return reject(s, "Это предприятие уже входит в империю")
        if (s.reputation < max(t.reputation, d.reputation)) return reject(s, "Повысьте репутацию для открытия")
        return spend(s, price(kind, district), "${t.name}: открытие состоялось") {
            addNews(it.copy(businesses = it.businesses + Business(kind, district), experience = it.experience + 100), "Новое предприятие", "${t.name} · ${d.name}")
        }
    }
    fun improve(s: GameState, kind: String, action: String): ActionResult {
        val b = s.businesses.find { it.kind == kind } ?: return reject(s, "Предприятие не найдено")
        val cost: Long
        val updated: Business
        when (action) {
            "level" -> { if (b.level >= 8) return reject(s, "Максимальный уровень"); cost = upgradeCost(b); updated = b.copy(level = b.level + 1) }
            "equipment" -> { if (b.equipment >= 5) return reject(s, "Оборудование максимального уровня"); cost = equipmentCost(b); updated = b.copy(equipment = b.equipment + 1) }
            "hire" -> { if (b.staff >= staffLimit(b)) return reject(s, "Команда укомплектована"); cost = hireCost(b); updated = b.copy(staff = b.staff + 1) }
            "fire" -> { if (b.staff <= 0) return reject(s, "Нет наёмных сотрудников"); cost = Catalog.type(b.kind).wage * 2; updated = b.copy(staff = b.staff - 1) }
            "ad" -> { if (b.advertisingUntil > s.day) return reject(s, "Кампания ещё действует"); cost = adCost(b); updated = b.copy(advertisingUntil = s.day + 7, popularity = (b.popularity + 4).coerceAtMost(100.0)) }
            else -> return reject(s, "Неизвестная операция")
        }
        return spend(s, cost, "Изменения применены") { it.copy(businesses = it.businesses.map { old -> if (old.kind == kind) updated else old },
            upgrades = it.upgrades + (if (action == "level" || action == "equipment") 1 else 0), experience = it.experience + 25) }
    }
    fun sell(s: GameState, kind: String): ActionResult {
        val b = s.businesses.find { it.kind == kind } ?: return reject(s, "Предприятие не найдено")
        if (s.businesses.size <= 1) return reject(s, "Сохраните хотя бы один бизнес")
        val proceeds = money(assetValue(b) * .55)
        return ActionResult(addNews(s.copy(cash = (s.cash + proceeds).coerceAtMost(MAX_MONEY), businesses = s.businesses.filterNot { it.kind == kind }), "Продажа предприятия", "${Catalog.type(kind).name} продано за $proceeds ₽"), "Предприятие продано")
    }
    fun relocate(s: GameState, kind: String, district: Int): ActionResult {
        val b = s.businesses.find { it.kind == kind } ?: return reject(s, "Предприятие не найдено")
        val d = Catalog.districts.getOrNull(district) ?: return reject(s, "Район не найден")
        if (district == b.district) return reject(s, "Бизнес уже в этом районе")
        if (s.reputation < d.reputation) return reject(s, "Недостаточно репутации")
        val cost = money(price(kind, district) * .18)
        return spend(s, cost, "Предприятие переехало") { it.copy(businesses = it.businesses.map { old -> if (old.kind == kind) old.copy(district = district) else old }) }
    }
    fun loanLimit(s: GameState): Long = min(500_000_000L, max(5_000L, money(netWorth(s).coerceAtLeast(0) * .60)))
    fun borrow(s: GameState, amount: Long, term: Int): ActionResult {
        if (s.bankrupt || s.cash < 0 || s.reputation < 15) return reject(s, "Банк временно отказал в кредите")
        if (s.loan != null) return reject(s, "Сначала погасите текущий кредит")
        if (term != 30 && term != 90) return reject(s, "Недопустимый срок")
        if (amount < 1_000 || amount > loanLimit(s)) return reject(s, "Сумма вне кредитного лимита")
        if (s.cash > MAX_MONEY - amount) return reject(s, "Достигнут предельный баланс")
        val total = money(amount * if (term == 30) 1.12 else 1.28)
        val loan = Loan(amount, total, ceil(total.toDouble() / term).toLong(), term, term)
        return ActionResult(addNews(s.copy(cash = s.cash + amount, loan = loan), "Кредит одобрен", "$amount ₽ на $term игровых дней. Полная стоимость: $total ₽."), "Деньги на счёте")
    }
    fun repay(s: GameState): ActionResult {
        val loan = s.loan ?: return reject(s, "Нет действующих кредитов")
        return spend(s, loan.remaining, "Кредит погашен досрочно") { it.copy(loan = null, reputation = (it.reputation + 1).coerceAtMost(100.0)) }
    }
    fun order(s: GameState, now: Long): ActionResult {
        if (s.bankrupt) return reject(s, "Предприятия остановлены")
        // Wall time is checked against the saved high-water clock, never a new session counter.
        val accepted = max(now.coerceAtMost(s.highWaterMs), 0)
        if (accepted - s.lastOrderMs < ORDER_COOLDOWN_MS) return reject(s, "Следующий заказ через несколько секунд")
        val reward = min(250_000L, max(45L, finances(s).revenue / 60))
        val tax = money(reward * .06)
        return ActionResult(s.copy(cash = (s.cash + reward - tax).coerceAtMost(MAX_MONEY), lastOrderMs = accepted,
            totalRevenue = (s.totalRevenue + reward).coerceAtMost(MAX_MONEY), totalTax = (s.totalTax + tax).coerceAtMost(MAX_MONEY),
            orders = s.orders + 1, experience = s.experience + 3), "Заказ выполнен: +${reward - tax} ₽ после налога")
    }
    fun invest(s: GameState, kind: Int, amount: Long): ActionResult {
        if (kind !in 0..2 || amount < 1_000 || amount > 100_000_000) return reject(s, "Допустимая инвестиция: от 1 000 до 100 млн ₽")
        if (s.investments.size >= 3) return reject(s, "Доступны три инвестиционных слота")
        val days = listOf(30, 45, 60)[kind]
        val hash = Math.floorMod(s.seed + s.day * 31 + s.investments.size * 19, 100L)
        val multiplier = when(kind) { 0 -> 1.08; 1 -> if(hash < 25) .85 else 1.30; else -> if(hash < 40) .60 else 1.70 }
        return spend(s, amount, "Инвестиция размещена") { it.copy(investments = it.investments + Investment(kind, amount, s.day + days, money(amount * multiplier))) }
    }
    fun questProgress(s: GameState, id: Int): Double = when(id) {
        0 -> (s.totalRevenue - s.questStartRevenue) / 1_000.0
        1 -> (s.orders - s.questStartOrders) / 3.0
        2 -> (s.upgrades - s.questStartUpgrades).toDouble()
        else -> 0.0
    }
    fun claimQuest(s: GameState, id: Int): ActionResult {
        if (id !in 0..2 || id in s.questClaimed || questProgress(s, id) < 1 || s.bankrupt) return reject(s, "Задание ещё не выполнено или награда получена")
        val reward = listOf(250L, 300L, 350L)[id]
        return ActionResult(s.copy(cash = (s.cash + reward).coerceAtMost(MAX_MONEY), questClaimed = s.questClaimed + id, experience = s.experience + 40), "Награда: +$reward ₽")
    }
    fun claimAchievement(s: GameState, id: String): ActionResult {
        val a = Catalog.achievements.find { it.id == id } ?: return reject(s, "Достижение не найдено")
        if (id in s.achievementsClaimed || a.progress(s) < 1 || s.bankrupt) return reject(s, "Награда пока недоступна")
        return ActionResult(s.copy(cash = (s.cash + a.reward).coerceAtMost(MAX_MONEY), achievementsClaimed = s.achievementsClaimed + id), "Достижение: +${a.reward} ₽")
    }
    fun recover(s: GameState): ActionResult {
        if (!s.bankrupt) return reject(s, "Реструктуризация не требуется")
        return ActionResult(addNews(s.copy(cash = 1_000, businesses = listOf(Business("coffee_stand")), loan = null,
            investments = emptyList(), bankrupt = false, bankruptcies = s.bankruptcies + 1, reputation = max(20.0, s.reputation - 20)),
            "Второй шанс", "Долги списаны, активы переданы кредиторам. Новая стойка и 1 000 ₽ — ваша точка роста."), "Вы снова в деле")
    }
}
