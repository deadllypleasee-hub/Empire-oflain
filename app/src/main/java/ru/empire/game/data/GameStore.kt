package ru.empire.game.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.flow.first
import ru.empire.core.GameState
import ru.empire.core.SaveCodec
import java.util.zip.CRC32

private val corrupt = booleanPreferencesKey("unrecoverable")
private val Context.empireStore by preferencesDataStore(
    name = "empire_v1",
    corruptionHandler = ReplaceFileCorruptionHandler {
        mutablePreferencesOf(corrupt to true)
    },
)

data class LoadedGame(val state: GameState?, val restoredBackup: Boolean = false)

/** DataStore atomically commits state and clock in the same transaction.
 * Both copies contain the SAME revision, so recovery cannot replay an old purchase. */
class GameStore(private val context: Context) {
    private val primary = stringPreferencesKey("snapshot")
    private val recovery = stringPreferencesKey("recovery")
    private fun pack(s: GameState): String {
        val payload = SaveCodec.encode(s)
        val crc = CRC32().apply { update(payload.toByteArray(Charsets.UTF_8)) }.value
        return "$crc\n$payload"
    }
    private fun unpack(raw: String): GameState {
        val split = raw.indexOf('\n')
        require(split > 0)
        val payload = raw.substring(split + 1)
        val actual = CRC32().apply { update(payload.toByteArray(Charsets.UTF_8)) }.value
        require(raw.substring(0, split).toLong() == actual) { "Повреждена контрольная сумма" }
        return SaveCodec.decode(payload)
    }
    suspend fun load(): LoadedGame {
        val p = context.empireStore.data.first()
        check(p[corrupt] != true) { "Файл прогресса повреждён. Для продолжения требуется явный сброс игры." }
        if (p[primary] == null && p[recovery] == null) return LoadedGame(null)
        p[primary]?.let { runCatching { unpack(it) }.getOrNull() }?.let { return LoadedGame(it) }
        p[recovery]?.let { runCatching { unpack(it) }.getOrNull() }?.let { return LoadedGame(it, true) }
        error("Не удалось прочитать сохранение. Повторите попытку или явно начните новую игру.")
    }
    suspend fun save(s: GameState) {
        val payload = pack(s)
        context.empireStore.edit { p -> p[primary] = payload; p[recovery] = payload; p.remove(corrupt) }
    }
}
