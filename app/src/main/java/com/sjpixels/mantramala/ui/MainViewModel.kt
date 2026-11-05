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
import android.net.Uri
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.data.MalaSource
import com.sjpixels.mantramala.data.VoiceCounterState
import com.sjpixels.mantramala.data.VoiceHistory
import com.sjpixels.mantramala.data.VoiceSample
import com.sjpixels.mantramala.voice.ImportResult
import com.sjpixels.mantramala.voice.VoicePlayback
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Tab { JAPA, MANTRAS, HISTORY, THEMES }

sealed interface UiEvent {
    data class RecordDeleted(val record: JapaRecord) : UiEvent
    /** A message (string resource) to show in a snackbar. */
    data class Message(val res: Int) : UiEvent
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
    private val voiceStore = container.voiceStore
    private val voicePlayer = container.voicePlayer

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

    private val records = japaDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Chanting stats for the Japa screen: tapped malas, plus played ones when histories are merged. */
    val stats: StateFlow<PracticeStats> = combine(records, _today, settings) { all, today, s ->
        StatsCalculator.compute(if (s.voiceHistory == VoiceHistory.MERGED) all else all.filter { !it.isVoice }, today)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, PracticeStats())

    /** Which history the History tab shows when chanted and played malas are kept separate. */
    private val _historyVoice = MutableStateFlow(false)
    val historyVoice: StateFlow<Boolean> = _historyVoice.asStateFlow()

    val historyStats: StateFlow<PracticeStats> = combine(records, _today, settings, _historyVoice) { all, today, s, voice ->
        StatsCalculator.compute(if (s.voiceHistory == VoiceHistory.MERGED) all else all.filter { it.isVoice == voice }, today)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, PracticeStats())


    fun showVoiceHistory(voice: Boolean) { _historyVoice.value = voice }

    /** Record sources the History tab currently acts on (for deleting). */
    private fun historySources(): List<String> = when {
        settings.value.voiceHistory == VoiceHistory.MERGED -> listOf(MalaSource.TAP, MalaSource.VOICE)
        _historyVoice.value -> listOf(MalaSource.VOICE)
        else -> listOf(MalaSource.TAP)
    }

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

    // --- Voice samples ---
    val voiceSamples: StateFlow<List<VoiceSample>> = container.database.voiceSampleDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Samples of the selected mantra; the most recently picked one plays. */
    val mantraSamples: StateFlow<List<VoiceSample>> = combine(voiceSamples, settings) { all, s ->
        all.filter { it.mantraKey == s.selectedMantraKey }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeSample: StateFlow<VoiceSample?> = mantraSamples.map { list -> list.maxByOrNull { it.selectedAt } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val voicePlayback: StateFlow<VoicePlayback> = voicePlayer.playback
    val voiceCounter: StateFlow<VoiceCounterState> = voicePlayer.counter

    fun newRecordingFile(): File = voiceStore.newRecordingFile()

    fun toggleVoice() {
        val sample = activeSample.value ?: return
        val p = voicePlayback.value
        if (p.sample?.id == sample.id && p.status != com.sjpixels.mantramala.voice.VoiceStatus.IDLE) voicePlayer.toggle() else voicePlayer.play(sample)
    }

    fun stopVoice() = voicePlayer.stop()

    fun resetVoiceCount() = voicePlayer.resetCount()

    fun selectSample(sample: VoiceSample) = viewModelScope.launch {
        container.database.voiceSampleDao().select(sample.id)
        val p = voicePlayback.value
        if (p.sample != null && p.sample.id != sample.id && p.status != com.sjpixels.mantramala.voice.VoiceStatus.IDLE) voicePlayer.play(sample)
    }

    fun deleteSample(sample: VoiceSample) = viewModelScope.launch {
        voicePlayer.stopIfPlaying(sample.id)
        voiceStore.delete(sample)
    }

    fun saveRecording(file: File, name: String) = viewModelScope.launch {
        val added = voiceStore.addRecording(settings.value.selectedMantraKey, name.trim().take(60), file)
        if (added == null) _events.emit(UiEvent.Message(R.string.voice_record_failed))
    }

    fun importSample(uri: Uri) = viewModelScope.launch {
        when (voiceStore.import(settings.value.selectedMantraKey, uri)) {
            is ImportResult.Added -> Unit
            ImportResult.TooLarge -> _events.emit(UiEvent.Message(R.string.voice_too_large))
            ImportResult.Unplayable -> _events.emit(UiEvent.Message(R.string.voice_unplayable))
        }
    }

    init {
        viewModelScope.launch {
            repo.settings.first()
            _settingsLoaded.value = true
        }
        // Switching to another mantra stops a recording of the previous one.
        viewModelScope.launch {
            settings.map { it.selectedMantraKey }.distinctUntilChanged().collect { voicePlayer.stopUnlessMantra(it) }
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
        voicePlayer.playback.value.sample?.takeIf { it.mantraKey == Mantras.customKey(mantra.id) }?.let { voicePlayer.stop() }
        voiceStore.deleteForMantra(Mantras.customKey(mantra.id))
        customDao.delete(mantra)
        if (settings.value.selectedMantraKey == Mantras.customKey(mantra.id)) repo.setMantra(Mantras.DEFAULT_KEY)
    }

    // --- History ---
    fun deleteRecord(record: JapaRecord) = viewModelScope.launch {
        japaDao.delete(record)
        _events.emit(UiEvent.RecordDeleted(record))
    }

    fun restoreRecord(record: JapaRecord) = viewModelScope.launch { japaDao.insert(record) }

    fun deleteDay(dayKey: Int) = viewModelScope.launch { japaDao.deleteDay(dayKey, historySources()) }

    /** Deletes history according to [range]; today's practice is kept unless EVERYTHING is chosen. */
    fun clearPast(range: ClearRange) = viewModelScope.launch {
        val keep = HistoryPruning.keepFrom(range, LocalDate.now())
        val sources = historySources()
        if (keep == null) japaDao.deleteAll(sources) else japaDao.deleteBefore(DayKeys.of(keep), sources)
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
    fun setVoicePauseEachMala(on: Boolean) = viewModelScope.launch { repo.setVoicePauseEachMala(on) }
    fun setVoiceHistory(mode: VoiceHistory) = viewModelScope.launch { repo.setVoiceHistory(mode) }
}
