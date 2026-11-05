package com.sjpixels.mantramala.data

import com.sjpixels.mantramala.util.Feedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Name snapshot stored with each mala, so history reads correctly even after a custom mantra is deleted. */
suspend fun resolveMantraLabel(key: String, customDao: CustomMantraDao): String =
    Mantras.customId(key)?.let { customDao.getById(it)?.name }
        ?: Mantras.builtIn(key)?.transliteration
        ?: key

/**
 * The one app-wide japa counter. The counter screen, the in-app floating bead and the
 * over-other-apps bead (FloatingCounterService) all count through here, so they always agree.
 * Must be called on the main thread.
 */
class CounterRepository(
    private val scope: CoroutineScope,
    private val settingsRepo: SettingsRepository,
    private val japaDao: JapaDao,
    private val customDao: CustomMantraDao,
    private val feedback: Feedback,
) {
    private val _counter = MutableStateFlow(CounterState())
    val counter: StateFlow<CounterState> = _counter.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Increments synchronously each time a mala completes; drives the celebration animation. */
    private val _completions = MutableStateFlow(0)
    val completions: StateFlow<Int> = _completions.asStateFlow()

    private val settings: StateFlow<UserSettings> =
        settingsRepo.settings.stateIn(scope, SharingStarted.Eagerly, UserSettings())

    init {
        scope.launch {
            _counter.value = settingsRepo.counter.first()
            _loaded.value = true
            // StateFlow is conflated: rapid taps never queue stale writes; the latest count always lands.
            _counter.drop(1).collect { settingsRepo.saveCounter(it) }
        }
    }

    fun increment() {
        if (!_loaded.value) return
        val now = System.currentTimeMillis()
        val result = CounterEngine.increment(_counter.value, now)
        _counter.value = result.state
        val s = settings.value
        if (result.malaCompleted) {
            feedback.malaComplete(haptics = s.haptics, bell = s.bell)
            _completions.value += 1
            val key = s.selectedMantraKey
            scope.launch {
                japaDao.insert(
                    JapaRecord(
                        mantraKey = key,
                        mantraLabel = resolveMantraLabel(key, customDao),
                        startedAt = result.roundStartedAt,
                        completedAt = now,
                        dayKey = DayKeys.fromMillis(now),
                    ),
                )
            }
        } else if (s.haptics) {
            feedback.bead()
        }
    }

    fun decrement() {
        if (!_loaded.value) return
        val before = _counter.value
        _counter.value = CounterEngine.decrement(before)
        if (before.count > 0 && settings.value.haptics) feedback.bead()
    }

    fun reset() {
        _counter.value = CounterState()
    }
}
