package ru.empire.core

import org.junit.Test
import kotlin.test.*

class GameClockTest {
    private val wall = 1_800_000_000_000L
    private fun started() = GameClock.advance(Economy.initial(wall), wall, 10_000L, 7).state

    @Test fun `changing wall time forward within a boot does not grant extra days`() {
        val r = GameClock.advance(started(), wall + 100 * 86_400_000L, 70_000L, 7)
        assertEquals(1, r.days)
        assertEquals(wall + 60_000, r.state.highWaterMs)
    }
    @Test fun `monotonic clock includes sleep while ignoring calendar rollback`() {
        val r = GameClock.advance(started(), wall - 86_400_000, 130_000L, 7)
        assertEquals(2, r.days)
        assertTrue(r.earned > 0)
    }
    @Test fun `restarting the process cannot replay elapsed realtime`() {
        val a = GameClock.advance(started(), wall + 120_000, 130_000L, 7).state
        val restored = SaveCodec.decode(SaveCodec.encode(a))
        assertEquals(restored, GameClock.advance(restored, wall + 120_000, 130_000L, 7).state)
    }
    @Test fun `device reboot falls back to capped high water clock`() {
        val a = GameClock.advance(started(), wall + 86_400_000, 5_000L, 8)
        assertTrue(a.capped)
        assertEquals(480, a.days)
        assertEquals(a.state, GameClock.advance(a.state, wall + 86_400_000, 5_000L, 8).state)
    }
    @Test fun `rollback across a reboot grants zero days and resumes monotonically`() {
        val a = GameClock.advance(started(), wall - 86_400_000, 5_000L, 8)
        assertEquals(0, a.days)
        val b = GameClock.advance(a.state, wall - 86_400_000 + 60_000, 65_000L, 8)
        assertEquals(1, b.days)
    }
    @Test fun `all six event types can appear in a long running game`() {
        val events = mutableSetOf<Int>()
        var s = Economy.initial(wall)
        repeat(100) {
            s = Economy.advance(s, s.highWaterMs + 7 * Economy.DAY_MS).state
            events += s.eventId
        }
        assertEquals(Catalog.events.indices.toSet(), events)
    }
}
