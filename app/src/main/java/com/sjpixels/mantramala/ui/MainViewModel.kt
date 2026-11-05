package com.sjpixels.mantramala.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sjpixels.mantramala.MantraMalaApp
import com.sjpixels.mantramala.data.CounterState
import com.sjpixels.mantramala.data.CounterStyle
import com.sjpixels.mantramala.data.CustomMantra
import com.sjpixels.mantramala.data.JapaRecord
import com.sjpixels.mantramala.data.Mantras
import com.sjpixels.mantramala.data.PracticeStats
import com.sjpixels.mantramala.data.StatsCalculator
import com.sjpixels.mantramala.data.ThemeMode
import com.sjpixels.mantramala.data.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import android.os.Build
import com.sjpixels.mantramala.data.ClearRange
import com.sjpixels.mantramala.data.DayKeys
import com.sjpixels.mantramala.data.HistoryPruning
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Tab { JAPA, MANTRAS, HISTORY, THEMES }

sealed interface UiEvent {
    data class RecordDeleted(val record: JapaRecord) : UiEvent
}

/** UI-facing view of either a built-in or a user-added mantra. */
data class MantraOption(
    val key: String,
    val builtInTitleRes: Int?,
    val customName: String?,
    val text: String,
    val transliteration: String?,
    val builtInMeaningRes: Int?,
    val customMeaning: String?,
    val custom: CustomMantra? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as MantraMalaApp).container
    private val repo = container.settings
    private val japaDao = container.database.japaDao()
    private val customDao = container.database.customMantraDao()
    private val counterRepo = container.counter

    private val _settingsLoaded = MutableStateFlow(false)

    val settings: StateFlow<UserSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    /** Keep the splash screen until saved theme + count are loaded, so nothing flickers. */
    val ready: StateFlow<Boolean> = combine(_settingsLoaded, counterRepo.loaded) { a, b -> a && b }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val counter: StateFlow<CounterState> = counterRepo.counter

    private val _tab = MutableStateFlow(Tab.JAPA)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    private val _today = MutableStateFlow(LocalDate.now())

    val completions: StateFlow<Int> = counterRepo.completions

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val stats: StateFlow<PracticeStats> = combine(japaDao.observeAll(), _today) { records, today ->
        StatsCalculator.compute(records, today)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, PracticeStats())

    val customMantras: StateFlow<List<CustomMantra>> = customDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val mantraOptions: StateFlow<List<MantraOption>> = customMantras.map { customs ->
        Mantras.builtIn.map {
            MantraOption(it.key, it.titleRes, null, it.devanagari, it.transliteration, it.meaningRes, null)
        } + customs.map {
            MantraOption(Mantras.customKey(it.id), null, it.name, it.text, null, null, it.meaning, it)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedMantra: StateFlow<MantraOption?> = combine(settings, mantraOptions) { s, options ->
        options.firstOrNull { it.key == s.selectedMantraKey } ?: options.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            repo.settings.first()
            _settingsLoaded.value = true
        }
    }

    fun selectTab(tab: Tab) { _tab.value = tab }

    fun refreshDay() { _today.value = LocalDate.now() }

    fun increment() = counterRepo.increment()

    fun decrement() = counterRepo.decrement()

    fun resetRound() = counterRepo.reset()

    // --- Mantras ---
    fun selectMantra(key: String) = viewModelScope.launch { repo.setMantra(key) }

    fun addCustomMantra(name: String, text: String, meaning: String) = viewModelScope.launch {
        val id = customDao.insert(CustomMantra(name = name.trim(), text = text.trim(), meaning = meaning.trim()))
        repo.setMantra(Mantras.customKey(id))
    }

    fun deleteCustomMantra(mantra: CustomMantra) = viewModelScope.launch {
        customDao.delete(mantra)
        if (settings.value.selectedMantraKey == Mantras.customKey(mantra.id)) repo.setMantra(Mantras.DEFAULT_KEY)
    }

    // --- History ---
    fun deleteRecord(record: JapaRecord) = viewModelScope.launch {
        japaDao.delete(record)
        _events.emit(UiEvent.RecordDeleted(record))
    }

    fun restoreRecord(record: JapaRecord) = viewModelScope.launch { japaDao.insert(record) }

    fun deleteDay(dayKey: Int) = viewModelScope.launch { japaDao.deleteDay(dayKey) }

    /** Deletes history according to [range]; today's practice is kept unless EVERYTHING is chosen. */
    fun clearPast(range: ClearRange) = viewModelScope.launch {
        val keep = HistoryPruning.keepFrom(range, LocalDate.now())
        if (keep == null) japaDao.deleteAll() else japaDao.deleteBefore(DayKeys.of(keep))
    }

    /** The day Mantra Mala was first installed on this phone (history is kept from then on, for life). */
    val installDate: LocalDate = runCatching {
        val pm = app.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(app.packageName, 0)
        }
        Instant.ofEpochMilli(info.firstInstallTime).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrElse { LocalDate.now() }

    // --- Settings ---
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setPalette(id: String) = viewModelScope.launch { repo.setPalette(id) }
    fun setCounterStyle(style: CounterStyle) = viewModelScope.launch { repo.setCounterStyle(style) }
    fun setDailyGoal(goal: Int) = viewModelScope.launch { repo.setDailyGoal(goal) }
    fun setHaptics(on: Boolean) = viewModelScope.launch { repo.setHaptics(on) }
    fun setBell(on: Boolean) = viewModelScope.launch { repo.setBell(on) }
    fun setVolumeKeys(on: Boolean) = viewModelScope.launch { repo.setVolumeKeys(on) }
    fun setKeepScreenOn(on: Boolean) = viewModelScope.launch { repo.setKeepScreenOn(on) }
    fun setFloatingButton(on: Boolean) = viewModelScope.launch { repo.setFloatingButton(on) }
    fun setFloatingColor(argb: Int) = viewModelScope.launch { repo.setFloatingColor(argb) }
    fun setFloatingOverlay(on: Boolean) = viewModelScope.launch { repo.setFloatingOverlay(on) }
    fun setFloatingSize(dp: Int) = viewModelScope.launch { repo.setFloatingSize(dp) }
    fun setFloatPosition(x: Float, y: Float) = viewModelScope.launch { repo.setFloatPosition(x, y) }
}
