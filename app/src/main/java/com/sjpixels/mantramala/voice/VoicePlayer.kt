package com.sjpixels.mantramala.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.sjpixels.mantramala.data.CounterEngine
import com.sjpixels.mantramala.data.CounterState
import com.sjpixels.mantramala.data.CustomMantraDao
import com.sjpixels.mantramala.data.DayKeys
import com.sjpixels.mantramala.data.JapaDao
import com.sjpixels.mantramala.data.JapaRecord
import com.sjpixels.mantramala.data.MalaSource
import com.sjpixels.mantramala.data.SettingsRepository
import com.sjpixels.mantramala.data.UserSettings
import com.sjpixels.mantramala.data.VoiceCounterState
import com.sjpixels.mantramala.data.VoiceSample
import com.sjpixels.mantramala.data.resolveMantraLabel
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

enum class VoiceStatus { IDLE, PLAYING, PAUSED }

data class VoicePlayback(
    val sample: VoiceSample? = null,
    val status: VoiceStatus = VoiceStatus.IDLE,
    /** Paused automatically because a mala of 108 plays just finished. */
    val pausedAfterMala: Boolean = false,
    /** The last attempt to play failed (missing or unreadable file). */
    val failed: Boolean = false,
)

/** Pure rules for the voice counter, kept free of Android so they can be unit tested. */
object VoiceRules {
    data class Step(val state: VoiceCounterState, val malaCompleted: Boolean, val roundStartedAt: Long, val keepPlaying: Boolean)

    /** The count to continue from when [mantraKey] starts playing: its saved round, or a fresh one for another mantra. */
    fun forMantra(saved: VoiceCounterState, mantraKey: String): VoiceCounterState =
        if (saved.mantraKey == mantraKey) saved else VoiceCounterState(mantraKey)

    /** One full play of the recording finished. */
    fun afterPlay(current: VoiceCounterState, now: Long, pauseEachMala: Boolean): Step {
        val r = CounterEngine.increment(CounterState(current.count, current.roundStartedAt), now)
        return Step(
            state = VoiceCounterState(current.mantraKey, r.state.count, r.state.roundStartedAt),
            malaCompleted = r.malaCompleted,
            roundStartedAt = r.roundStartedAt,
            keepPlaying = !(r.malaCompleted && pauseEachMala),
        )
    }
}

/**
 * Plays the chosen voice sample on repeat and counts every complete play, separately from the tap counter.
 * Every 108 plays is saved as a mala marked [MalaSource.VOICE]. App-wide, so playback continues with the
 * screen off; [VoicePlaybackService] keeps the process in the foreground meanwhile. Main thread only.
 */
class VoicePlayer(
    context: Context,
    private val scope: CoroutineScope,
    private val settingsRepo: SettingsRepository,
    private val japaDao: JapaDao,
    private val customDao: CustomMantraDao,
    private val store: VoiceStore,
    private val feedback: Feedback,
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)

    private val _playback = MutableStateFlow(VoicePlayback())
    val playback: StateFlow<VoicePlayback> = _playback.asStateFlow()

    private val _counter = MutableStateFlow(VoiceCounterState())
    val counter: StateFlow<VoiceCounterState> = _counter.asStateFlow()

    /** Bumps on every voice mala, for UI celebration. */
    private val _completions = MutableStateFlow(0)
    val completions: StateFlow<Int> = _completions.asStateFlow()

    private val settings: StateFlow<UserSettings> =
        settingsRepo.settings.stateIn(scope, SharingStarted.Eagerly, UserSettings())

    private var player: MediaPlayer? = null
    private var loaded = false
    private var resumeOnFocusGain = false
    private var hasFocus = false

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> { resumeOnFocusGain = false; hasFocus = false; pause() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val wasPlaying = _playback.value.status == VoiceStatus.PLAYING
                pause()
                resumeOnFocusGain = wasPlaying
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player?.setVolume(0.3f, 0.3f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                player?.setVolume(1f, 1f)
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    resume()
                }
            }
        }
    }

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener(focusListener)
        .setWillPauseWhenDucked(false)
        .build()

    init {
        scope.launch {
            _counter.value = settingsRepo.voiceCounter.first()
            loaded = true
            _counter.drop(1).collect { settingsRepo.saveVoiceCounter(it) }
        }
    }

    /** Plays [sample] from where it was (same sample) or from the start (new sample). Call from a visible screen. */
    fun play(sample: VoiceSample) {
        val current = _playback.value
        if (current.sample?.id == sample.id && player != null) {
            resume()
            return
        }
        releasePlayer()
        val file = store.file(sample)
        if (!file.exists()) {
            _playback.value = VoicePlayback(sample, VoiceStatus.IDLE, failed = true)
            return
        }
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(attributes)
            mp.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
            mp.setDataSource(file.absolutePath)
            mp.prepare()
        } catch (_: Exception) {
            mp.release()
            _playback.value = VoicePlayback(sample, VoiceStatus.IDLE, failed = true)
            return
        }
        mp.setOnCompletionListener { onPlayFinished() }
        mp.setOnErrorListener { _, _, _ ->
            releasePlayer()
            _playback.value = _playback.value.copy(status = VoiceStatus.IDLE, failed = true)
            abandonFocus()
            true
        }
        player = mp
        if (loaded) _counter.value = VoiceRules.forMantra(_counter.value, sample.mantraKey)
        _playback.value = VoicePlayback(sample, VoiceStatus.PAUSED)
        ensureService()
        resume()
    }

    fun resume() {
        val mp = player ?: return
        if (!hasFocus) {
            hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!hasFocus) return
        }
        if (_playback.value.pausedAfterMala) runCatching { mp.seekTo(0) }
        if (loaded && _counter.value.mantraKey != _playback.value.sample?.mantraKey) {
            _playback.value.sample?.let { _counter.value = VoiceRules.forMantra(_counter.value, it.mantraKey) }
        }
        runCatching { mp.start() }
        _playback.value = _playback.value.copy(status = VoiceStatus.PLAYING, pausedAfterMala = false, failed = false)
    }

    fun pause() {
        val mp = player ?: return
        resumeOnFocusGain = false
        runCatching { if (mp.isPlaying) mp.pause() }
        if (_playback.value.status == VoiceStatus.PLAYING) {
            _playback.value = _playback.value.copy(status = VoiceStatus.PAUSED)
        }
    }

    fun toggle() {
        when (_playback.value.status) {
            VoiceStatus.PLAYING -> pause()
            VoiceStatus.PAUSED -> resume()
            VoiceStatus.IDLE -> _playback.value.sample?.let(::play)
        }
    }

    /** Stops playback and ends the background service. The voice count is kept. */
    fun stop() {
        releasePlayer()
        abandonFocus()
        _playback.value = VoicePlayback(_playback.value.sample, VoiceStatus.IDLE)
    }

    fun resetCount() {
        _counter.value = VoiceCounterState(_counter.value.mantraKey)
        if (_playback.value.pausedAfterMala) _playback.value = _playback.value.copy(pausedAfterMala = false)
    }

    /** Stops if the given sample (being deleted) or anything of another mantra is playing. */
    fun stopIfPlaying(sampleId: Long) {
        if (_playback.value.sample?.id == sampleId) stop()
    }

    fun stopUnlessMantra(mantraKey: String) {
        val s = _playback.value.sample ?: return
        if (s.mantraKey != mantraKey && _playback.value.status != VoiceStatus.IDLE) stop()
    }

    private fun onPlayFinished() {
        val sample = _playback.value.sample ?: return
        val now = System.currentTimeMillis()
        val s = settings.value
        val step = VoiceRules.afterPlay(VoiceRules.forMantra(_counter.value, sample.mantraKey), now, s.voicePauseEachMala)
        _counter.value = step.state
        if (step.malaCompleted) {
            _completions.value += 1
            feedback.malaComplete(haptics = false, bell = s.bell)
            scope.launch {
                japaDao.insert(
                    JapaRecord(
                        mantraKey = sample.mantraKey,
                        mantraLabel = resolveMantraLabel(sample.mantraKey, customDao),
                        startedAt = step.roundStartedAt,
                        completedAt = now,
                        dayKey = DayKeys.fromMillis(now),
                        source = MalaSource.VOICE,
                    ),
                )
            }
        }
        val mp = player ?: return
        if (step.keepPlaying) {
            runCatching { mp.seekTo(0); mp.start() }
        } else {
            _playback.value = _playback.value.copy(status = VoiceStatus.PAUSED, pausedAfterMala = true)
        }
    }

    private fun ensureService() {
        if (VoicePlaybackService.running) return
        runCatching {
            ContextCompat.startForegroundService(appContext, Intent(appContext, VoicePlaybackService::class.java))
        }
    }

    private fun releasePlayer() {
        player?.let { mp ->
            runCatching { mp.stop() }
            mp.release()
        }
        player = null
    }

    private fun abandonFocus() {
        resumeOnFocusGain = false
        if (hasFocus) audioManager.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
    }
}
