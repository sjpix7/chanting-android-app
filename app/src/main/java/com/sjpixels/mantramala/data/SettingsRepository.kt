package com.sjpixels.mantramala.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val MALA_SIZE = 108
const val FLOATING_SIZE_MIN = 48
const val FLOATING_SIZE_MAX = 112
const val FLOATING_SIZE_DEFAULT = 64

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class CounterStyle { MALA, PADMA, YANTRA }

/** Whether malas played from a voice sample share the history of chanted malas or keep their own. */
enum class VoiceHistory { MERGED, SEPARATE }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteId: String = "saffron",
    val counterStyle: CounterStyle = CounterStyle.MALA,
    val selectedMantraKey: String = Mantras.DEFAULT_KEY,
    val dailyGoal: Int = 1,
    val haptics: Boolean = true,
    val bell: Boolean = true,
    val volumeKeys: Boolean = false,
    val keepScreenOn: Boolean = true,
    /** Movable counter bead shown on every screen. */
    val floatingButton: Boolean = true,
    /** ARGB colour of the bead; 0 = follow the theme. */
    val floatingColor: Int = 0,
    /** Keep the bead over other apps (needs "display over other apps"). */
    val floatingOverlay: Boolean = false,
    /** Bead diameter in dp. */
    val floatingSize: Int = FLOATING_SIZE_DEFAULT,
    /** Bead position as fractions (0..1) of the available width / height. */
    val floatX: Float = 0.97f,
    val floatY: Float = 0.66f,
    /** Voice playback stops after every 108 plays; off = keep playing and counting. */
    val voicePauseEachMala: Boolean = true,
    val voiceHistory: VoiceHistory = VoiceHistory.SEPARATE,
)

/** The in-progress round. Persisted on every bead so nothing is lost if the app is closed. */
data class CounterState(val count: Int = 0, val roundStartedAt: Long = 0L)

/** The in-progress round of voice plays, for one mantra. */
data class VoiceCounterState(val mantraKey: String = "", val count: Int = 0, val roundStartedAt: Long = 0L)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val palette = stringPreferencesKey("palette")
        val counterStyle = stringPreferencesKey("counter_style")
        val mantra = stringPreferencesKey("mantra")
        val dailyGoal = intPreferencesKey("daily_goal")
        val haptics = booleanPreferencesKey("haptics")
        val bell = booleanPreferencesKey("bell")
        val volumeKeys = booleanPreferencesKey("volume_keys")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val count = intPreferencesKey("current_count")
        val roundStart = longPreferencesKey("round_started_at")
        val floatingButton = booleanPreferencesKey("floating_button")
        val floatingColor = intPreferencesKey("floating_color")
        val floatingOverlay = booleanPreferencesKey("floating_overlay")
        val floatingSize = intPreferencesKey("floating_size")
        val floatX = floatPreferencesKey("float_x")
        val floatY = floatPreferencesKey("float_y")
        val voicePause = booleanPreferencesKey("voice_pause_each_mala")
        val voiceHistory = stringPreferencesKey("voice_history")
        val voiceMantra = stringPreferencesKey("voice_count_mantra")
        val voiceCount = intPreferencesKey("voice_count")
        val voiceRoundStart = longPreferencesKey("voice_round_started_at")
    }

    val settings: Flow<UserSettings> = store.data.map { p ->
        val d = UserSettings()
        UserSettings(
            themeMode = p[Keys.themeMode].toEnum(d.themeMode),
            paletteId = p[Keys.palette] ?: d.paletteId,
            counterStyle = p[Keys.counterStyle].toEnum(d.counterStyle),
            selectedMantraKey = p[Keys.mantra] ?: d.selectedMantraKey,
            dailyGoal = p[Keys.dailyGoal] ?: d.dailyGoal,
            haptics = p[Keys.haptics] ?: d.haptics,
            bell = p[Keys.bell] ?: d.bell,
            volumeKeys = p[Keys.volumeKeys] ?: d.volumeKeys,
            keepScreenOn = p[Keys.keepScreenOn] ?: d.keepScreenOn,
            floatingButton = p[Keys.floatingButton] ?: d.floatingButton,
            floatingColor = p[Keys.floatingColor] ?: d.floatingColor,
            floatingOverlay = p[Keys.floatingOverlay] ?: d.floatingOverlay,
            floatingSize = (p[Keys.floatingSize] ?: d.floatingSize).coerceIn(FLOATING_SIZE_MIN, FLOATING_SIZE_MAX),
            floatX = (p[Keys.floatX] ?: d.floatX).coerceIn(0f, 1f),
            floatY = (p[Keys.floatY] ?: d.floatY).coerceIn(0f, 1f),
            voicePauseEachMala = p[Keys.voicePause] ?: d.voicePauseEachMala,
            voiceHistory = p[Keys.voiceHistory].toEnum(d.voiceHistory),
        )
    }

    val voiceCounter: Flow<VoiceCounterState> = store.data.map { p ->
        VoiceCounterState(
            mantraKey = p[Keys.voiceMantra] ?: "",
            count = (p[Keys.voiceCount] ?: 0).coerceIn(0, MALA_SIZE - 1),
            roundStartedAt = p[Keys.voiceRoundStart] ?: 0L,
        )
    }

    suspend fun saveVoiceCounter(state: VoiceCounterState) = store.edit {
        it[Keys.voiceMantra] = state.mantraKey
        it[Keys.voiceCount] = state.count
        it[Keys.voiceRoundStart] = state.roundStartedAt
    }

    suspend fun setVoicePauseEachMala(on: Boolean) = store.edit { it[Keys.voicePause] = on }
    suspend fun setVoiceHistory(mode: VoiceHistory) = store.edit { it[Keys.voiceHistory] = mode.name }

    val counter: Flow<CounterState> = store.data.map { p ->
        CounterState(
            count = (p[Keys.count] ?: 0).coerceIn(0, MALA_SIZE - 1),
            roundStartedAt = p[Keys.roundStart] ?: 0L,
        )
    }

    suspend fun saveCounter(state: CounterState) = store.edit {
        it[Keys.count] = state.count
        it[Keys.roundStart] = state.roundStartedAt
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.themeMode] = mode.name }
    suspend fun setPalette(id: String) = store.edit { it[Keys.palette] = id }
    suspend fun setCounterStyle(style: CounterStyle) = store.edit { it[Keys.counterStyle] = style.name }
    suspend fun setMantra(key: String) = store.edit { it[Keys.mantra] = key }
    suspend fun setDailyGoal(goal: Int) = store.edit { it[Keys.dailyGoal] = goal.coerceIn(1, 108) }
    suspend fun setHaptics(on: Boolean) = store.edit { it[Keys.haptics] = on }
    suspend fun setBell(on: Boolean) = store.edit { it[Keys.bell] = on }
    suspend fun setVolumeKeys(on: Boolean) = store.edit { it[Keys.volumeKeys] = on }
    suspend fun setKeepScreenOn(on: Boolean) = store.edit { it[Keys.keepScreenOn] = on }
    suspend fun setFloatingButton(on: Boolean) = store.edit { it[Keys.floatingButton] = on }
    suspend fun setFloatingColor(argb: Int) = store.edit { it[Keys.floatingColor] = argb }
    suspend fun setFloatingOverlay(on: Boolean) = store.edit { it[Keys.floatingOverlay] = on }
    suspend fun setFloatingSize(dp: Int) = store.edit { it[Keys.floatingSize] = dp.coerceIn(FLOATING_SIZE_MIN, FLOATING_SIZE_MAX) }
    suspend fun setFloatPosition(x: Float, y: Float) = store.edit {
        it[Keys.floatX] = x.coerceIn(0f, 1f)
        it[Keys.floatY] = y.coerceIn(0f, 1f)
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
