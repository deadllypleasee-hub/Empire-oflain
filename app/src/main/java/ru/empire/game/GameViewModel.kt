package ru.empire.game

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.empire.core.*
import ru.empire.game.data.GameStore
import ru.empire.game.data.ReminderWorker

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val store = GameStore(app)
    private val mutex = Mutex()
    private val mutable = MutableStateFlow<GameState?>(null)
    val state = mutable.asStateFlow()
    private val errors = MutableStateFlow<String?>(null)
    val error = errors.asStateFlow()
    private val events = MutableSharedFlow<String>(extraBufferCapacity = 12)
    val messages = events.asSharedFlow()
    private val _offline = MutableStateFlow<AdvanceResult?>(null)
    val offline = _offline.asStateFlow()
    private var ticker: Job? = null
    private var tone: ToneGenerator? = null
    private fun advance(s: GameState): AdvanceResult {
        val boot = runCatching { Settings.Global.getInt(getApplication<Application>().contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(-1)
        return GameClock.advance(s, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot)
    }

    init { load() }
    fun load() = viewModelScope.launch {
        mutex.withLock {
            try {
                val loaded = withContext(Dispatchers.IO) { store.load() }
                val now = System.currentTimeMillis()
                val result = withContext(Dispatchers.Default) { advance(loaded.state ?: Economy.initial(now)) }
                withContext(Dispatchers.IO) { store.save(result.state) }
                mutable.value = result.state
                errors.value = null
                if (loaded.state != null && result.days >= 2) _offline.value = result
                if (loaded.restoredBackup) events.tryEmit("Сохранение восстановлено из резервной копии")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                errors.value = e.message ?: "Ошибка чтения сохранения"
            }
        }
    }
    private suspend fun transact(action: ((GameState) -> ActionResult)? = null) = mutex.withLock {
        val old = mutable.value ?: return@withLock
        try {
            val advanced = advance(old).state
            val result = action?.invoke(advanced) ?: ActionResult(advanced, "")
            // Never expose an economy mutation before durable persistence succeeds.
            withContext(Dispatchers.IO) { store.save(result.state) }
            mutable.value = result.state
            errors.value = null
            if (result.message.isNotEmpty()) events.tryEmit(result.message)
            if (action != null && result.success && result.state.settings.sound) {
                runCatching {
                    if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, 22)
                    tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 55)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            errors.value = "Не удалось сохранить прогресс. Проверьте свободное место и повторите."
        }
    }
    fun act(action: (GameState) -> ActionResult) { viewModelScope.launch { transact(action) } }
    fun foreground() {
        ReminderWorker.cancel(getApplication())
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch { while (isActive) { transact(); delay(1_000) } }
    }
    fun background() {
        ticker?.cancel(); ticker = null
        viewModelScope.launch {
            transact()
            ReminderWorker.schedule(getApplication(), mutable.value?.settings?.notifications == true)
        }
    }
    fun dismissOffline() { _offline.value = null }
    fun saveNow() = act { ActionResult(it, "Прогресс сохранён на устройстве") }
    fun tutorial(step: Int) = act { ActionResult(it.copy(tutorialStep = step.coerceIn(0, 4)), "") }
    fun sound(value: Boolean) = act { ActionResult(it.copy(settings = it.settings.copy(sound = value)), "") }
    fun notifications(value: Boolean) {
        act { ActionResult(it.copy(settings = it.settings.copy(notifications = value)), if (value) "Напоминание через 8 часов после выхода" else "Напоминания отключены") }
        if (!value) ReminderWorker.cancel(getApplication())
    }
    fun reset() { viewModelScope.launch {
        mutex.withLock {
            try {
                val s = Economy.initial(System.currentTimeMillis())
                withContext(Dispatchers.IO) { store.save(s) }
                mutable.value = s; errors.value = null; _offline.value = null
                ReminderWorker.cancel(getApplication())
                events.tryEmit("Новая история вашей империи")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                errors.value = "Не удалось начать новую игру: ошибка записи"
            }
        }
    } }
    override fun onCleared() { tone?.release(); super.onCleared() }
}
