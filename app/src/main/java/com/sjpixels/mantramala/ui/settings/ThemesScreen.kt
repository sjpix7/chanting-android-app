package com.sjpixels.mantramala.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.sharp.ColorLens
import androidx.compose.material.icons.sharp.Layers
import androidx.compose.material.icons.sharp.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sjpixels.mantramala.overlay.FloatingOverlay
import com.sjpixels.mantramala.ui.components.beadInk
import com.sjpixels.mantramala.data.FLOATING_SIZE_MAX
import com.sjpixels.mantramala.data.FLOATING_SIZE_MIN
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.VolumeUp
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.CloudOff
import androidx.compose.material.icons.sharp.Flag
import androidx.compose.material.icons.sharp.NotificationsActive
import androidx.compose.material.icons.sharp.Remove
import androidx.compose.material.icons.sharp.Smartphone
import androidx.compose.material.icons.sharp.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sjpixels.mantramala.BuildConfig
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.data.CounterStyle
import com.sjpixels.mantramala.data.ThemeMode
import com.sjpixels.mantramala.data.VoiceHistory
import androidx.compose.material.icons.sharp.History
import androidx.compose.material.icons.sharp.PauseCircle
import com.sjpixels.mantramala.ui.MainViewModel
import com.sjpixels.mantramala.ui.components.BuiltByLink
import com.sjpixels.mantramala.ui.components.LanguageButton
import com.sjpixels.mantramala.ui.components.OrnamentDivider
import com.sjpixels.mantramala.ui.components.SectionHeader
import com.sjpixels.mantramala.ui.components.SharpCard
import com.sjpixels.mantramala.ui.counter.DialArt
import com.sjpixels.mantramala.ui.theme.LocalDarkTheme
import com.sjpixels.mantramala.ui.theme.Palettes

@Composable
fun ThemesScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDarkTheme.current

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.themes_title), style = MaterialTheme.typography.headlineSmall, color = scheme.onBackground)
                Spacer(Modifier.height(8.dp))
                OrnamentDivider()
            }
        }

        // ---- Day / night
        item { SectionHeader(stringResource(R.string.section_appearance)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeMode.SYSTEM to R.string.mode_system,
                    ThemeMode.LIGHT to R.string.mode_day,
                    ThemeMode.DARK to R.string.mode_night,
                ).forEach { (mode, label) ->
                    ChoiceChip(stringResource(label), s.themeMode == mode, Modifier.weight(1f)) { vm.setThemeMode(mode) }
                }
            }
        }

        // ---- Palettes
        item { SectionHeader(stringResource(R.string.section_colour_theme)) }
        Palettes.all.chunked(3).forEach { row ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { p ->
                        val selected = p.id == s.paletteId
                        val main = if (dark) p.primaryNight else p.primary
                        val acc = if (dark) p.accentNight else p.accent
                        Column(
                            Modifier.weight(1f).clickable(role = Role.RadioButton) { vm.setPalette(p.id) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.6f)
                                    .background(Brush.linearGradient(listOf(main, p.deep)), CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
                                    .border(
                                        if (selected) 2.5.dp else 1.dp,
                                        if (selected) scheme.onBackground else scheme.outlineVariant,
                                        CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(Modifier.size(14.dp).background(acc, CutCornerShape(50)))
                                if (selected) Icon(Icons.Sharp.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(p.nameRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) scheme.onBackground else scheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // ---- Counter style
        item { SectionHeader(stringResource(R.string.section_counter_style)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    CounterStyle.MALA to R.string.style_mala,
                    CounterStyle.PADMA to R.string.style_padma,
                    CounterStyle.YANTRA to R.string.style_yantra,
                ).forEach { (style, label) ->
                    val selected = s.counterStyle == style
                    SharpCard(
                        Modifier.weight(1f).clickable(role = Role.RadioButton) { vm.setCounterStyle(style) },
                        highlighted = selected,
                        contentPadding = 10.dp,
                    ) {
                        DialArt(progress = 40f, style = style, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // ---- Language
        item { SectionHeader(stringResource(R.string.section_language)) }
        item { LanguageButton(Modifier.fillMaxWidth(), expandedWidth = true) }

        // ---- Practice
        item { SectionHeader(stringResource(R.string.section_practice)) }
        item {
            SharpCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Sharp.Flag, contentDescription = null, tint = scheme.secondary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.daily_goal), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.daily_goal_desc), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vm.setDailyGoal(s.dailyGoal - 1) }, enabled = s.dailyGoal > 1) {
                        Icon(Icons.Sharp.Remove, contentDescription = stringResource(R.string.action_decrease))
                    }
                    Text(s.dailyGoal.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                    IconButton(onClick = { vm.setDailyGoal(s.dailyGoal + 1) }, enabled = s.dailyGoal < 108) {
                        Icon(Icons.Sharp.Add, contentDescription = stringResource(R.string.action_increase))
                    }
                }
                Divider()
                ToggleRow(Icons.Sharp.Vibration, R.string.setting_vibration, R.string.setting_vibration_desc, s.haptics, vm::setHaptics)
                Divider()
                ToggleRow(Icons.Sharp.NotificationsActive, R.string.setting_bell, R.string.setting_bell_desc, s.bell, vm::setBell)
                Divider()
                ToggleRow(Icons.AutoMirrored.Sharp.VolumeUp, R.string.setting_volume_keys, R.string.setting_volume_keys_desc, s.volumeKeys, vm::setVolumeKeys)
                Divider()
                ToggleRow(Icons.Sharp.Smartphone, R.string.setting_keep_screen_on, R.string.setting_keep_screen_on_desc, s.keepScreenOn, vm::setKeepScreenOn)
            }
        }

        // ---- Voice playback
        item { SectionHeader(stringResource(R.string.voice_channel_name)) }
        item {
            SharpCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                ToggleRow(Icons.Sharp.PauseCircle, R.string.setting_voice_pause, R.string.setting_voice_pause_desc, s.voicePauseEachMala, vm::setVoicePauseEachMala)
                Divider()
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Sharp.History, contentDescription = null, tint = scheme.secondary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.setting_voice_history), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.setting_voice_history_desc), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().padding(start = 38.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceChip(stringResource(R.string.voice_history_separate), s.voiceHistory == VoiceHistory.SEPARATE, Modifier.weight(1f)) {
                            vm.setVoiceHistory(VoiceHistory.SEPARATE)
                        }
                        ChoiceChip(stringResource(R.string.voice_history_merged), s.voiceHistory == VoiceHistory.MERGED, Modifier.weight(1f)) {
                            vm.setVoiceHistory(VoiceHistory.MERGED)
                        }
                    }
                }
            }
        }

        // ---- Floating counter
        item { SectionHeader(stringResource(R.string.section_floating)) }
        item { FloatingCounterSettings(vm, s) }

        // ---- About
        item { SectionHeader(stringResource(R.string.section_about)) }
        item {
            SharpCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Sharp.CloudOff, contentDescription = null, tint = scheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.about_offline), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text(stringResource(R.string.about_fonts), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                OrnamentDivider(Modifier.align(Alignment.CenterHorizontally), width = 80.dp)
                Spacer(Modifier.height(6.dp))
                BuiltByLink(Modifier.align(Alignment.CenterHorizontally), emphasis = 0.9f)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

private val beadSwatches = listOf(
    0xFFE8590C, 0xFFC62828, 0xFFD4A017, 0xFF1E4FA8, 0xFF2E7D32, 0xFFD63384,
    0xFF7A3E1F, 0xFF00897B, 0xFF6A1B9A, 0xFF0288D1, 0xFF212121, 0xFFF5F0E6,
).map { it.toInt() }

@Composable
private fun FloatingCounterSettings(vm: MainViewModel, s: com.sjpixels.mantramala.data.UserSettings) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var canOverlay by remember { mutableStateOf(FloatingOverlay.canDrawOverlays(context)) }
    var askPermission by rememberSaveable { mutableStateOf(false) }
    var pendingOverlay by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Returning from Android's "display over other apps" screen: finish turning the option on.
    LifecycleResumeEffect(Unit) {
        canOverlay = FloatingOverlay.canDrawOverlays(context)
        if (pendingOverlay && canOverlay) {
            pendingOverlay = false
            vm.setFloatingOverlay(true)
            askForNotifications()
        }
        onPauseOrDispose { }
    }

    SharpCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
        ToggleRow(Icons.Sharp.TouchApp, R.string.setting_floating_show, R.string.setting_floating_show_desc, s.floatingButton, vm::setFloatingButton)
        Divider()
        val enabled = s.floatingButton
        Column(Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.4f).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Sharp.ColorLens, contentDescription = null, tint = scheme.secondary)
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.floating_colour), style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChoiceChip(stringResource(R.string.floating_colour_theme), s.floatingColor == 0, Modifier.width(92.dp)) {
                    if (enabled) vm.setFloatingColor(0)
                }
                beadSwatches.forEach { argb ->
                    val selected = s.floatingColor == argb
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(Color(argb), CircleShape)
                            .border(if (selected) 3.dp else 1.dp, if (selected) scheme.onBackground else scheme.outlineVariant, CircleShape)
                            .clickable(enabled = enabled, role = Role.RadioButton) { vm.setFloatingColor(argb) },
                    ) {
                        if (selected) Icon(Icons.Sharp.Check, contentDescription = null, tint = beadInk(Color(argb)), modifier = Modifier.align(Alignment.Center).size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Any colour: a hue slider (fixed saturation/brightness keeps the bead vivid and readable).
            val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(if (s.floatingColor != 0) s.floatingColor else scheme.primary.toArgb(), it) }
            var hue by remember(s.floatingColor) { mutableFloatStateOf(hsv[0]) }
            val preview = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.78f, 0.86f)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.floating_colour_hue), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    // Rainbow track drawn behind a transparent-track slider.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(8.dp)
                            .background(
                                Brush.horizontalGradient((0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 59f, 0.78f, 0.86f))) }),
                                CutCornerShape(2.dp),
                            ),
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        onValueChangeFinished = { vm.setFloatingColor(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.78f, 0.86f))) },
                        valueRange = 0f..354f,
                        enabled = enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = preview,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            disabledActiveTrackColor = Color.Transparent,
                            disabledInactiveTrackColor = Color.Transparent,
                        ),
                    )
                }
            }
            // Size
            var sizeDp by remember(s.floatingSize) { mutableFloatStateOf(s.floatingSize.toFloat()) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.floating_size), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
                Slider(
                    value = sizeDp,
                    onValueChange = { sizeDp = it },
                    onValueChangeFinished = { vm.setFloatingSize(sizeDp.roundToInt()) },
                    valueRange = FLOATING_SIZE_MIN.toFloat()..FLOATING_SIZE_MAX.toFloat(),
                    steps = (FLOATING_SIZE_MAX - FLOATING_SIZE_MIN) / 8 - 1,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                    // Live preview, scaled down so the largest size fits the row.
                    val previewColor = if (s.floatingColor != 0) Color(s.floatingColor) else scheme.primary
                    Box(Modifier.size((sizeDp * 0.45f).dp).background(previewColor, CircleShape))
                }
            }
        }
        Divider()
        val needsPermission = s.floatingOverlay && !canOverlay
        Row(
            Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.4f)
                .toggleable(value = s.floatingOverlay && canOverlay, enabled = enabled, role = Role.Switch) { on ->
                    when {
                        !on -> vm.setFloatingOverlay(false)
                        canOverlay -> { vm.setFloatingOverlay(true); askForNotifications() }
                        else -> askPermission = true
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Sharp.Layers, contentDescription = null, tint = scheme.secondary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.setting_floating_overlay), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(if (needsPermission) R.string.floating_permission_needed else R.string.setting_floating_overlay_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (needsPermission) scheme.error else scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = s.floatingOverlay && canOverlay, onCheckedChange = null, enabled = enabled)
        }
    }

    if (askPermission) {
        AlertDialog(
            onDismissRequest = { askPermission = false },
            icon = { Icon(Icons.Sharp.Layers, contentDescription = null) },
            title = { Text(stringResource(R.string.overlay_permission_title)) },
            text = { Text(stringResource(R.string.overlay_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    askPermission = false
                    pendingOverlay = true
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
                    )
                }) { Text(stringResource(R.string.action_open_settings)) }
            },
            dismissButton = { TextButton(onClick = { askPermission = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Divider() = androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 54.dp))

@Composable
private fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
        color = if (selected) scheme.primary else scheme.surfaceContainerHigh,
        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, scheme.outlineVariant),
        modifier = modifier.height(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: Int, desc: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = scheme.secondary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}
