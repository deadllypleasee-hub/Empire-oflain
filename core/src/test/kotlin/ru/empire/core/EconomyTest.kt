package ru.empire.core

import kotlin.test.*
import org.junit.Test

class EconomyTest {
    private val start = 1_800_000_000_000L
    private fun state() = Economy.initial(start)

    @Test fun `player starts with one thousand and a running coffee stand`() {
        val s = state()
        assertEquals(1_000, s.cash)
        assertEquals("coffee_stand", s.businesses.single().kind)
        assertTrue(Economy.finances(s).profit > 300)
    }
    @Test fun `catalog has fifteen unique businesses with healthy unit economics`() {
        assertEquals(15, Catalog.businesses.map { it.id }.distinct().size)
        Catalog.businesses.forEach {
            val f = Economy.finance(Business(it.id), state())
            assertTrue(f.profit > 0, it.name)
            assertTrue(it.price / f.profit in 5..120, "Payback ${it.name}")
        }
    }
    @Test fun `every district stays profitable at maximum staffing and equipment`() {
        Catalog.businesses.forEach { t -> Catalog.districts.indices.forEach { d ->
            val b = Business(t.id, d, level = 8, equipment = 5, staff = 10, popularity = 100.0)
            assertTrue(Economy.finance(b, state()).profit > 0, "${t.id} $d")
        } }
    }
    @Test fun `fractional game days are conserved across saves`() {
        val a = Economy.advance(state(), start + 35_000).state
        val b = Economy.advance(SaveCodec.decode(SaveCodec.encode(a)), start + 60_000).state
        assertEquals(2, b.day)
        assertEquals(0, b.remainderMs)
        assertEquals(Economy.advance(state(), start + 60_000).state.cash, b.cash)
    }
    @Test fun `reopening at same timestamp cannot double income`() {
        val first = Economy.advance(state(), start + 600_000).state
        assertEquals(first, Economy.advance(first, start + 600_000).state)
    }
    @Test fun `clock rollback keeps high water and does not credit money`() {
        val first = Economy.advance(state(), start + 600_000).state
        val rollback = Economy.advance(first, start - 999_999).state
        assertEquals(first, rollback)
        assertEquals(first, Economy.advance(rollback, start + 599_999).state)
    }
    @Test fun `long absence is capped and discarded time is never replayed`() {
        val result = Economy.advance(state(), start + 10 * 86_400_000)
        assertTrue(result.capped)
        assertEquals(480, result.days)
        assertEquals(result.state, Economy.advance(result.state, start + 10 * 86_400_000).state)
    }
    @Test fun `foreground and offline simulation produce identical balances`() {
        var s = state()
        repeat(120) { i -> s = Economy.advance(s, start + (i + 1) * 60_000).state }
        val offline = Economy.advance(state(), start + 120 * 60_000).state
        assertEquals(s.cash, offline.cash)
        assertEquals(s.history, offline.history)
        assertEquals(s.news, offline.news)
    }
    @Test fun `cash expenses taxes and loan installment reconcile`() {
        val s = Economy.borrow(state(), 1_000, 30).state
        val r = Economy.advance(s, start + 60_000).state
        val h = r.history.last()
        assertEquals(h.revenue - h.rent - h.wages - h.service - h.supplies - h.tax - h.debt, h.net)
        assertEquals(s.cash + h.net, r.cash)
        assertEquals(h.tax, r.totalTax)
    }
    @Test fun `buy charges exact amount and cannot duplicate or overspend`() {
        val s = state().copy(cash = 10_000)
        val bought = Economy.buy(s, "kiosk", 0)
        assertTrue(bought.success)
        assertEquals(4_000, bought.state.cash)
        assertFalse(Economy.buy(bought.state, "kiosk", 0).success)
        assertFalse(Economy.buy(state(), "kiosk", 0).success)
        assertFalse(Economy.buy(s, "bad", 0).success)
        assertFalse(Economy.buy(s, "kiosk", 99).success)
    }
    @Test fun `reputation gates businesses and districts`() {
        assertFalse(Economy.buy(state().copy(cash = 100_000_000), "hotel", 0).success)
        assertFalse(Economy.buy(state().copy(cash = 100_000_000), "kiosk", 3).success)
    }
    @Test fun `upgrades equipment and employees have real marginal benefit`() {
        var s = state().copy(cash = 100_000)
        for (action in listOf("level", "equipment", "hire", "ad")) {
            val before = Economy.finances(s).profit
            val result = Economy.improve(s, "coffee_stand", action)
            assertTrue(result.success)
            assertTrue(result.state.cash < s.cash)
            assertTrue(Economy.finances(result.state).profit > before, action)
            s = result.state
        }
        assertFalse(Economy.improve(s, "coffee_stand", "ad").success)
    }
    @Test fun `loan pays fixed total on time without extra interest`() {
        var s = Economy.borrow(state(), 3_000, 30).state
        assertEquals(3_360, s.loan!!.remaining)
        assertEquals(112, s.loan!!.installment)
        assertFalse(Economy.borrow(s, 1_000, 90).success)
        s = Economy.advance(s, start + 30 * Economy.DAY_MS).state
        assertNull(s.loan)
        assertEquals(3_360, s.history.sumOf { it.debt })
    }
    @Test fun `loan limit and early payoff are enforced`() {
        assertFalse(Economy.borrow(state(), 1_000_000, 30).success)
        assertFalse(Economy.borrow(state(), -100, 30).success)
        assertFalse(Economy.borrow(state(), 1_000, 0).success)
        val borrowed = Economy.borrow(state(), 1_000, 90).state
        val repaid = Economy.repay(borrowed)
        assertTrue(repaid.success)
        assertEquals(720, repaid.state.cash)
        assertNull(repaid.state.loan)
    }
    @Test fun `bankruptcy stops operations and recovery retains time and achievements`() {
        var s = state().copy(cash = -100_000, achievementsClaimed = setOf("first_sale"))
        s = Economy.advance(s, start + Economy.DAY_MS).state
        assertTrue(s.bankrupt)
        assertFalse(Economy.buy(s, "kiosk", 0).success)
        val stopped = Economy.advance(s, start + Economy.DAY_MS * 10).state
        assertEquals(s.cash, stopped.cash)
        val recovered = Economy.recover(stopped).state
        assertFalse(recovered.bankrupt)
        assertEquals(1_000, recovered.cash)
        assertEquals(stopped.highWaterMs, recovered.highWaterMs)
        assertEquals(s.achievementsClaimed, recovered.achievementsClaimed)
    }
    @Test fun `manual orders cannot be repeated across restart or rollback`() {
        val first = Economy.order(state(), start).state
        assertTrue(first.cash > state().cash)
        assertFalse(Economy.order(SaveCodec.decode(SaveCodec.encode(first)), start).success)
        assertFalse(Economy.order(first, start - 100_000).success)
        assertFalse(Economy.order(first, start + 100_000).success) // must advance saved clock first
    }
    @Test fun `daily rewards are claimed once and reset only on a new UTC date`() {
        val s = state().copy(orders = 3)
        val r = Economy.claimQuest(s, 1)
        assertTrue(r.success)
        assertFalse(Economy.claimQuest(r.state, 1).success)
        assertEquals(r.state.questClaimed, Economy.advance(r.state, start - 86_400_000).state.questClaimed)
        val tomorrow = Economy.advance(r.state, start + 86_400_000).state
        assertTrue(tomorrow.questClaimed.isEmpty())
        assertFalse(Economy.claimQuest(tomorrow, 1).success)
    }
    @Test fun `achievement reward cannot be collected twice`() {
        val s = state().copy(totalRevenue = 1_000)
        val r = Economy.claimAchievement(s, "first_sale")
        assertTrue(r.success)
        assertEquals(1_400, r.state.cash)
        assertFalse(Economy.claimAchievement(r.state, "first_sale").success)
    }
    @Test fun `bonds debit immediately and pay exactly once at maturity`() {
        var s = Economy.invest(state().copy(cash = 10_000), 0, 5_000).state
        assertEquals(5_000, s.cash)
        val baseline = Economy.advance(s.copy(investments = emptyList()), start + 30 * Economy.DAY_MS).state
        s = Economy.advance(s, start + 30 * Economy.DAY_MS).state
        assertEquals(baseline.cash + 5_400, s.cash)
        assertTrue(s.investments.isEmpty())
        assertEquals(s, Economy.advance(s, s.highWaterMs).state)
    }
    @Test fun `events are deterministic and expire`() {
        val a = Economy.advance(state(), start + 6 * Economy.DAY_MS).state
        val b = Economy.advance(state(), start + 6 * Economy.DAY_MS).state
        assertNotNull(Economy.activeEvent(a))
        assertEquals(a.eventId, b.eventId)
        val expired = Economy.advance(a, start + 9 * Economy.DAY_MS).state
        assertNull(Economy.activeEvent(expired))
    }
    @Test fun `last business cannot be sold and selling never creates arbitrage`() {
        assertFalse(Economy.sell(state(), "coffee_stand").success)
        val s = state().copy(cash = 20_000)
        val bought = Economy.buy(s, "kiosk", 0).state
        val sold = Economy.sell(bought, "kiosk").state
        assertTrue(sold.cash < s.cash)
    }
    @Test fun `save roundtrip preserves fractional time debts and all flags`() {
        val s = Economy.borrow(state(), 1_000, 90).state.copy(remainderMs = 59_999, tutorialStep = 4, settings = Settings(false, true))
        assertEquals(s, SaveCodec.decode(SaveCodec.encode(s)))
        assertFails { SaveCodec.decode("{}") }
        assertFails { SaveCodec.decode(SaveCodec.encode(s.copy(version = 99))) }
        assertFails { SaveCodec.decode(SaveCodec.encode(s.copy(businesses = listOf(Business("unknown"))))) }
    }
    @Test fun `free progression can buy all fifteen businesses and reach one billion`() {
        var s = state()
        var minutes = 0
        while (s.businesses.size < 15 && minutes < 10_000) {
            val next = Catalog.businesses.firstOrNull { t -> s.businesses.none { it.kind == t.id } }
            if (next != null && s.cash >= next.price && s.reputation >= next.reputation) s = Economy.buy(s, next.id, 0).state
            // Conservative reinvestment: upgrade only when 2x cost is in cash.
            for (b in s.businesses.toList()) {
                if (b.level < 4 && s.cash > Economy.upgradeCost(b) * 2) s = Economy.improve(s, b.kind, "level").state
                val current = s.businesses.first { it.kind == b.kind }
                if (current.equipment < 3 && s.cash > Economy.equipmentCost(current) * 2) s = Economy.improve(s, b.kind, "equipment").state
            }
            s = Economy.advance(s, s.highWaterMs + Economy.DAY_MS).state
            assertFalse(s.bankrupt)
            assertTrue(s.cash >= 0)
            minutes++
        }
        assertEquals(15, s.businesses.size, "Progress after $minutes minutes")
        assertTrue(Economy.netWorth(s) >= 1_000_000_000)
        println("Free progression: all businesses in $minutes game days / real minutes; capital=${Economy.netWorth(s)}")
    }
}
