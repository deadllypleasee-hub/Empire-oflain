package ru.empire.core

import kotlinx.serialization.Serializable

@Serializable
data class Business(
    val kind: String,
    val district: Int = 0,
    val level: Int = 1,
    val equipment: Int = 0,
    val staff: Int = 0,
    val popularity: Double = 25.0,
    val advertisingUntil: Long = 0,
    val lifetimeProfit: Long = 0,
)

@Serializable
data class Loan(val principal: Long, val remaining: Long, val installment: Long, val daysLeft: Int, val term: Int)

@Serializable
data class Investment(val kind: Int, val amount: Long, val dueDay: Long, val payout: Long)

@Serializable
data class DayReport(val day: Long, val revenue: Long, val rent: Long, val wages: Long, val service: Long, val supplies: Long, val tax: Long, val debt: Long, val net: Long)

@Serializable
data class News(val title: String, val detail: String, val day: Long)

@Serializable
data class Settings(val sound: Boolean = true, val notifications: Boolean = false)

@Serializable
data class GameState(
    val version: Int = 1,
    val cash: Long = 1_000,
    val businesses: List<Business> = listOf(Business("coffee_stand")),
    val day: Long = 1,
    val remainderMs: Long = 0,
    val highWaterMs: Long,
    val clockElapsedMs: Long = -1,
    val clockBootId: Int = -1,
    val reputation: Double = 30.0,
    val experience: Long = 0,
    val totalRevenue: Long = 0,
    val totalTax: Long = 0,
    val orders: Int = 0,
    val upgrades: Int = 0,
    val bankruptcies: Int = 0,
    val loan: Loan? = null,
    val investments: List<Investment> = emptyList(),
    val history: List<DayReport> = emptyList(),
    val news: List<News> = listOf(News("Добро пожаловать в EMPIRE", "Кофейная стойка уже открыта. Ваш первый миллион начинается здесь.", 1)),
    val eventId: Int = -1,
    val eventUntil: Long = 0,
    val seed: Long = 714_503,
    val lastOrderMs: Long = 0,
    val questEpoch: Long = 0,
    val questStartRevenue: Long = 0,
    val questStartOrders: Int = 0,
    val questStartUpgrades: Int = 0,
    val questClaimed: Set<Int> = emptySet(),
    val achievementsClaimed: Set<String> = emptySet(),
    val tutorialStep: Int = 0,
    val bankrupt: Boolean = false,
    val settings: Settings = Settings(),
)

data class BusinessType(
    val id: String, val name: String, val subtitle: String, val price: Long,
    val dailyRevenue: Long, val rent: Long, val wage: Long, val service: Long,
    val supplyRate: Double, val reputation: Int, val icon: String, val color: Long,
)

data class District(val name: String, val description: String, val reputation: Int, val priceFactor: Double, val salesFactor: Double, val rentFactor: Double)

data class EconomyEvent(val title: String, val description: String, val sales: Double, val costs: Double, val good: Boolean)

data class Finance(val revenue: Long, val rent: Long, val wages: Long, val service: Long, val supplies: Long, val tax: Long) {
    val expenses get() = rent + wages + service + supplies + tax
    val profit get() = revenue - expenses
}

data class Achievement(val id: String, val title: String, val description: String, val reward: Long, val progress: (GameState) -> Double)

data class ActionResult(val state: GameState, val message: String, val success: Boolean = true)
data class AdvanceResult(val state: GameState, val days: Int, val earned: Long, val capped: Boolean = false)
