package ru.empire.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SaveCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(state: GameState): String = json.encodeToString(state)
    fun decode(payload: String): GameState {
        require(payload.length <= 2_000_000) { "Слишком большое сохранение" }
        val s = json.decodeFromString<GameState>(payload)
        require(s.version == 1) { "Неизвестная версия сохранения" }
        require(s.cash in -Economy.MAX_MONEY..Economy.MAX_MONEY)
        require(s.day in 1..1_000_000_000 && s.highWaterMs >= 0)
        require(s.remainderMs in 0 until Economy.DAY_MS)
        require(s.reputation.isFinite() && s.reputation in 0.0..100.0)
        require(s.businesses.isNotEmpty() && s.businesses.size <= Catalog.businesses.size)
        require(s.businesses.map { it.kind }.distinct().size == s.businesses.size)
        s.businesses.forEach { b ->
            require(Catalog.businesses.any { it.id == b.kind })
            require(b.district in Catalog.districts.indices && b.level in 1..8 && b.equipment in 0..5)
            require(b.staff in 0..Economy.staffLimit(b) && b.popularity.isFinite() && b.popularity in 0.0..100.0)
        }
        s.loan?.let { require(it.principal > 0 && it.remaining > 0 && it.installment > 0 && it.daysLeft in 1..90 && it.term in listOf(30, 90)) }
        require(s.investments.size <= 3 && s.investments.all { it.kind in 0..2 && it.amount in 1_000..100_000_000 && it.payout >= 0 })
        require(s.history.size <= 30 && s.news.size <= 25 && s.eventId in -1 until Catalog.events.size)
        return s
    }
}
