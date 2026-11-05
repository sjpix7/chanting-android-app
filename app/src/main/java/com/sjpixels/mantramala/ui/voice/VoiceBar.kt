package com.sjpixels.mantramala.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.KeyboardArrowRight
import androidx.compose.material.icons.sharp.AudioFile
import androidx.compose.material.icons.sharp.DeleteOutline
import androidx.compose.material.icons.sharp.FiberManualRecord
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.Mic
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.RestartAlt
import androidx.compose.material.icons.sharp.Stop
import androidx.compose.material.icons.sharp.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.data.MALA_SIZE
import com.sjpixels.mantramala.data.VoiceSample
import com.sjpixels.mantramala.ui.MainViewModel
import com.sjpixels.mantramala.ui.components.SharpCard
import com.sjpixels.mantramala.voice.VoiceRecorder
import com.sjpixels.mantramala.voice.VoiceStatus
import kotlinx.coroutines.delay
import java.io.File

/**
 * Voice playback for the selected mantra on the Japa screen: play/pause the chosen voice sample on repeat,
 * with its own count of plays (separate from the tap counter), or add a sample if there is none yet.
 */
@Composable
fun VoiceBar(vm: MainViewModel, mantraTitle: String, modifier: Modifier = Modifier) {
    val samples by vm.mantraSamples.collectAsStateWithLifecycle()
    val active by vm.activeSample.collectAsStateWithLifecycle()
    val playback by vm.voicePlayback.collectAsStateWithLifecycle()
    val counter by vm.voiceCounter.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val today by vm.voiceTodayMalas.collectAsStateWithLifecycle()
    var managing by rememberSaveable { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    val sample = active
    if (sample == null) {
        Surface(
            onClick = { managing = true },
            shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
            color = scheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Sharp.Mic, contentDescription = null, tint = scheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.voice_add), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                    Text(stringResource(R.string.voice_add_desc), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2)
                }
                Icon(Icons.AutoMirrored.Sharp.KeyboardArrowRight, contentDescription = null, tint = scheme.primary)
            }
        }
    } else {
        val mine = playback.sample?.id == sample.id
        val status = if (mine) playback.status else VoiceStatus.IDLE
        val count = if (counter.mantraKey == sample.mantraKey) counter.count else 0
        SharpCard(modifier.fillMaxWidth(), contentPadding = 8.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = vm::toggleVoice,
                    shape = CircleShape,
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (status == VoiceStatus.PLAYING) {
                            Icon(Icons.Sharp.Pause, contentDescription = stringResource(R.string.voice_pause))
                        } else {
                            Icon(Icons.Sharp.PlayArrow, contentDescription = stringResource(R.string.voice_play))
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { managing = true }) {
                    Text(sample.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val line = when {
                        mine && playback.failed -> stringResource(R.string.voice_missing)
                        mine && playback.pausedAfterMala -> stringResource(R.string.voice_mala_done)
                        else -> stringResource(R.string.voice_count, count, MALA_SIZE) +
                            if (today > 0) " · " + pluralStringResource(R.plurals.malas_today, today, today) else ""
                    }
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mine && playback.failed) scheme.error else scheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { count / MALA_SIZE.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = scheme.primary,
                        trackColor = scheme.outlineVariant,
                        strokeCap = StrokeCap.Butt,
                        drawStopIndicator = {},
                    )
                }
                if (status != VoiceStatus.IDLE) {
                    IconButton(onClick = vm::stopVoice) {
                        Icon(Icons.Sharp.Stop, contentDescription = stringResource(R.string.voice_stop), tint = scheme.onSurfaceVariant)
                    }
                } else {
                    IconButton(onClick = vm::resetVoiceCount, enabled = count > 0) {
                        Icon(Icons.Sharp.RestartAlt, contentDescription = stringResource(R.string.voice_reset), tint = scheme.onSurfaceVariant.copy(alpha = if (count > 0) 1f else 0.38f))
                    }
                }
                IconButton(onClick = { managing = true }) {
                    Icon(Icons.Sharp.LibraryMusic, contentDescription = stringResource(R.string.voice_manage), tint = scheme.primary)
                }
            }
        }
    }

    if (managing) {
        VoiceSamplesDialog(
            vm = vm,
            mantraTitle = mantraTitle,
            samples = samples,
            activeId = active?.id,
            onDismiss = { managing = false },
        )
    }
}

@Composable
private fun VoiceSamplesDialog(
    vm: MainViewModel,
    mantraTitle: String,
    samples: List<VoiceSample>,
    activeId: Long?,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var recording by rememberSaveable { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<VoiceSample?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importSample(uri)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Sharp.LibraryMusic, contentDescription = null) },
        title = {
            Column {
                Text(stringResource(R.string.voice_samples_title))
                Text(mantraTitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (samples.isEmpty()) {
                    Text(stringResource(R.string.voice_samples_empty), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
                samples.forEach { s ->
                    val selected = s.id == activeId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { vm.selectSample(s) })
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            if (s.recorded) Icons.Sharp.Mic else Icons.Sharp.AudioFile,
                            contentDescription = null,
                            tint = scheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(clock(s.durationMs), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { toDelete = s }) {
                            Icon(Icons.Sharp.DeleteOutline, contentDescription = stringResource(R.string.action_delete), tint = scheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { recording = true }, shape = CutCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Sharp.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.voice_record), maxLines = 1)
                    }
                    OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }, shape = CutCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Sharp.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.voice_upload), maxLines = 1)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )

    if (recording) {
        val defaultName = stringResource(R.string.voice_default_name, samples.count { it.recorded } + 1)
        RecordDialog(
            vm = vm,
            defaultName = defaultName,
            onDismiss = { recording = false },
            onSave = { file, name -> vm.saveRecording(file, name.ifBlank { defaultName }); recording = false },
        )
    }

    toDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.voice_delete_title)) },
            text = { Text(stringResource(R.string.voice_delete_message, s.name)) },
            confirmButton = {
                TextButton(onClick = { vm.deleteSample(s); toDelete = null }) {
                    Text(stringResource(R.string.action_delete), color = scheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private enum class RecState { READY, RECORDING, DONE }

/** Record once (up to 10 minutes), listen back, name it, save. */
@Composable
private fun RecordDialog(vm: MainViewModel, defaultName: String, onDismiss: () -> Unit, onSave: (File, String) -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val recorder = remember { VoiceRecorder(context) }
    var state by remember { mutableStateOf(RecState.READY) }
    var file by remember { mutableStateOf<File?>(null) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var level by remember { mutableFloatStateOf(0f) }
    var name by remember { mutableStateOf(defaultName) }
    var micDenied by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<MediaPlayer?>(null) }
    var saved by remember { mutableStateOf(false) }

    fun stopPreview() {
        preview?.let { runCatching { it.stop() }; it.release() }
        preview = null
    }

    fun begin() {
        stopPreview()
        file?.delete()
        file = null
        vm.stopVoice() // otherwise the microphone records the playback too
        val target = vm.newRecordingFile()
        if (recorder.start(target)) {
            failed = false
            elapsed = 0L
            state = RecState.RECORDING
        } else {
            failed = true
        }
    }

    fun finish() {
        val f = recorder.stop()
        if (f != null) {
            file = f
            state = RecState.DONE
        } else {
            failed = true
            state = RecState.READY
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micDenied = !granted
        if (granted) begin()
    }

    fun startWithPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            begin()
        } else {
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(state) {
        if (state != RecState.RECORDING) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (state == RecState.RECORDING) {
            elapsed = System.currentTimeMillis() - start
            level = recorder.level()
            if (elapsed >= VoiceRecorder.MAX_DURATION_MS) finish()
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
            recorder.cancel()
            if (!saved) file?.delete()
        }
    }

    AlertDialog(
        onDismissRequest = { if (state != RecState.RECORDING) onDismiss() },
        icon = { Icon(Icons.Sharp.Mic, contentDescription = null) },
        title = { Text(stringResource(if (state == RecState.RECORDING) R.string.voice_recording else R.string.voice_record)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (state) {
                    RecState.READY -> {
                        Text(stringResource(R.string.voice_record_hint), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Surface(onClick = ::startWithPermission, shape = CircleShape, color = scheme.error, contentColor = scheme.onError, modifier = Modifier.size(72.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Sharp.FiberManualRecord, contentDescription = stringResource(R.string.voice_record), modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                    RecState.RECORDING -> {
                        Text(clock(elapsed), style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { level },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = scheme.error,
                            trackColor = scheme.outlineVariant,
                            strokeCap = StrokeCap.Butt,
                            drawStopIndicator = {},
                        )
                        Spacer(Modifier.height(16.dp))
                        Surface(onClick = ::finish, shape = CircleShape, color = scheme.error, contentColor = scheme.onError, modifier = Modifier.size(72.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Sharp.Stop, contentDescription = stringResource(R.string.voice_stop), modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                    RecState.DONE -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = {
                                    if (preview != null) {
                                        stopPreview()
                                    } else {
                                        file?.let { f ->
                                            runCatching {
                                                MediaPlayer().apply {
                                                    setDataSource(f.absolutePath)
                                                    prepare()
                                                    setOnCompletionListener { stopPreview() }
                                                    start()
                                                }
                                            }.onSuccess { preview = it }
                                        }
                                    }
                                },
                                shape = CircleShape, color = scheme.primary, contentColor = scheme.onPrimary, modifier = Modifier.size(48.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (preview != null) Icon(Icons.Sharp.Stop, contentDescription = stringResource(R.string.voice_stop))
                                    else Icon(Icons.Sharp.PlayArrow, contentDescription = stringResource(R.string.voice_play))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(clock(elapsed), style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = ::startWithPermission) { Text(stringResource(R.string.voice_rerecord)) }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = name, onValueChange = { name = it.take(60) },
                            label = { Text(stringResource(R.string.voice_name)) },
                            singleLine = true, shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (micDenied) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.voice_mic_denied), style = MaterialTheme.typography.bodySmall, color = scheme.error)
                }
                if (failed) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.voice_record_failed), style = MaterialTheme.typography.bodySmall, color = scheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = state == RecState.DONE,
                onClick = {
                    stopPreview()
                    file?.let { saved = true; onSave(it, name) }
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = { if (state == RecState.RECORDING) recorder.cancel(); onDismiss() }) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private fun clock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
