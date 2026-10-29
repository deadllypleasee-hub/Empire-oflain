package ru.empire.core

/** Wall time is needed only across device boots. Within a boot, elapsedRealtime
 * includes deep sleep and cannot be accelerated by changing the calendar. */
object GameClock {
    fun advance(s: GameState, wallMs: Long, elapsedMs: Long, bootId: Int): AdvanceResult {
        val sameBoot = bootId >= 0 && bootId == s.clockBootId &&
            s.clockElapsedMs >= 0 && elapsedMs >= s.clockElapsedMs
        val now = if (sameBoot) {
            val delta = elapsedMs - s.clockElapsedMs
            if (delta > Long.MAX_VALUE - s.highWaterMs) Long.MAX_VALUE else s.highWaterMs + delta
        } else wallMs
        val result = Economy.advance(s, now)
        return result.copy(state = result.state.copy(clockElapsedMs = elapsedMs, clockBootId = bootId))
    }
}
