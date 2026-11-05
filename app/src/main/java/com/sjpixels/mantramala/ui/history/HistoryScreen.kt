package com.sjpixels.mantramala.ui.history

import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.sharp.ExpandLess
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.Role
import com.sjpixels.mantramala.data.ClearRange
import com.sjpixels.mantramala.data.HistoryPruning
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CalendarMonth
import androidx.compose.material.icons.sharp.DeleteOutline
import androidx.compose.material.icons.sharp.DeleteSweep
import androidx.compose.material.icons.sharp.EmojiEvents
import androidx.compose.material.icons.sharp.Grain
import androidx.compose.material.icons.sharp.LocalFireDepartment
import androidx.compose.material.icons.sharp.Spa
import androidx.compose.material.icons.sharp.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.data.DayBar
import com.sjpixels.mantramala.data.DayGroup
import com.sjpixels.mantramala.data.JapaRecord
import com.sjpixels.mantramala.data.MALA_SIZE
import com.sjpixels.mantramala.data.Mantras
import com.sjpixels.mantramala.ui.MainViewModel
import com.sjpixels.mantramala.ui.components.MandalaBackdrop
import com.sjpixels.mantramala.ui.components.OrnamentDivider
import com.sjpixels.mantramala.ui.components.SectionHeader
import com.sjpixels.mantramala.ui.components.SharpCard
import com.sjpixels.mantramala.ui.counter.StatTile
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val stats by vm.stats.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ROOT
    val nf = remember(locale) { NumberFormat.getIntegerInstance(locale) }

    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var confirmDay by rememberSaveable { mutableStateOf<Int?>(null) }
    /** Months whose open/closed state the user flipped from the default (comma-separated yyyyMM). */
    var toggledMonths by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(4.dp))
                // Lifetime record: from the earlier of install day and the first mala (restored backups can be older).
                val since = stats.firstDay?.let { minOf(it, vm.installDate) } ?: vm.installDate
                val daysOnPath = (ChronoUnit.DAYS.between(since, LocalDate.now()) + 1).toInt().coerceAtLeast(1)
                Text(
                    stringResource(
                        R.string.history_since,
                        since.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                        pluralStringResource(R.plurals.days_short, daysOnPath, daysOnPath),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                OrnamentDivider()
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(Icons.Sharp.Spa, stringResource(R.string.stat_total_malas), nf.format(stats.totalMalas), Modifier.weight(1f))
                    StatTile(Icons.Sharp.Grain, stringResource(R.string.stat_total_japa), nf.format(stats.totalJapa), Modifier.weight(1f))
                    StatTile(Icons.Sharp.CalendarMonth, stringResource(R.string.stat_active_days), nf.format(stats.activeDays), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(Icons.Sharp.LocalFireDepartment, stringResource(R.string.stat_current_streak), pluralStringResource(R.plurals.days_short, stats.currentStreak, stats.currentStreak), Modifier.weight(1f))
                    StatTile(Icons.Sharp.EmojiEvents, stringResource(R.string.stat_best_streak), pluralStringResource(R.plurals.days_short, stats.bestStreak, stats.bestStreak), Modifier.weight(1f))
                    StatTile(Icons.Sharp.Timer, stringResource(R.string.stat_avg_time), if (stats.avgMalaMillis > 0) formatDuration(stats.avgMalaMillis) else "—", Modifier.weight(1f))
                }
            }
        }
        item {
            SharpCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.chart_last_7_days), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(R.string.chart_caption, settings.dailyGoal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                WeekChart(stats.last7Days, settings.dailyGoal, locale)
            }
        }

        if (stats.groups.isEmpty()) {
            item { EmptyHistory() }
        } else {
            item { SectionHeader(stringResource(R.string.section_record)) }
            // Whole lifetime, grouped by month. The latest month starts open; tap a month to fold/unfold it.
            val months = stats.groups.groupBy { it.dayKey / 100 }.entries.toList()
            val latestMonth = months.first().key
            months.forEach { (monthKey, days) ->
                val open = (monthKey == latestMonth) xor (monthKey.toString() in toggledMonths.split(','))
                item(key = "month-$monthKey") {
                    MonthHeader(
                        month = YearMonth.of(monthKey / 100, monthKey % 100),
                        malas = days.sumOf { it.records.size },
                        open = open,
                        locale = locale,
                        onToggle = {
                            val set = toggledMonths.split(',').filter { it.isNotEmpty() }.toMutableSet()
                            if (!set.add(monthKey.toString())) set.remove(monthKey.toString())
                            toggledMonths = set.joinToString(",")
                        },
                    )
                }
                if (open) {
                    days.forEach { group ->
                        item(key = "day-${group.dayKey}") {
                            DayHeader(group, locale, onDelete = { confirmDay = group.dayKey })
                        }
                        items(group.records, key = { "rec-${it.id}" }) { record ->
                            RecordRow(record, locale, onDelete = { vm.deleteRecord(record) })
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { confirmClear = true },
                    shape = CutCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Sharp.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.clear_past_history), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmClear) {
        ClearPastDialog(
            groups = stats.groups,
            onDismiss = { confirmClear = false },
            onConfirm = { range -> vm.clearPast(range); confirmClear = false },
        )
    }
    confirmDay?.let { key ->
        val group = stats.groups.firstOrNull { it.dayKey == key }
        if (group == null) {
            confirmDay = null
        } else {
            AlertDialog(
                onDismissRequest = { confirmDay = null },
                title = { Text(stringResource(R.string.delete_day_title)) },
                text = { Text(pluralStringResource(R.plurals.delete_day_message, group.records.size, group.records.size, dayLabel(group.date, locale))) },
                confirmButton = {
                    TextButton(onClick = { vm.deleteDay(key); confirmDay = null }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { confirmDay = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, malas: Int, open: Boolean, locale: Locale, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pattern = remember(locale) { android.text.format.DateFormat.getBestDateTimePattern(locale, "yMMMM") }
    val label = remember(month, pattern) {
        month.format(DateTimeFormatter.ofPattern(pattern, locale)).replaceFirstChar { it.titlecase(locale) }
    }
    Surface(
        onClick = onToggle,
        shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
        color = scheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Text(pluralStringResource(R.plurals.malas_count, malas, malas), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Icon(if (open) Icons.Sharp.ExpandLess else Icons.Sharp.ExpandMore, contentDescription = null, tint = scheme.primary)
        }
    }
}

@Composable
private fun ClearPastDialog(groups: List<DayGroup>, onDismiss: () -> Unit, onConfirm: (ClearRange) -> Unit) {
    var choice by rememberSaveable { mutableStateOf(ClearRange.BEFORE_TODAY) }
    val today = LocalDate.now()
    val options = listOf(
        ClearRange.BEFORE_TODAY to R.string.clear_option_before_today,
        ClearRange.OLDER_THAN_30_DAYS to R.string.clear_option_older_30,
        ClearRange.OLDER_THAN_1_YEAR to R.string.clear_option_older_365,
        ClearRange.EVERYTHING to R.string.clear_option_all,
    )
    val count = HistoryPruning.countToDelete(groups, choice, today)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Sharp.DeleteSweep, contentDescription = null) },
        title = { Text(stringResource(R.string.clear_past_history)) },
        text = {
            Column {
                options.forEach { (range, label) ->
                    val n = HistoryPruning.countToDelete(groups, range, today)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = choice == range, role = Role.RadioButton, onClick = { choice = range })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == range, onClick = null)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(pluralStringResource(R.plurals.malas_count, n, n), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (count == 0) stringResource(R.string.nothing_to_clear)
                    else pluralStringResource(R.plurals.clear_selected_message, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (count == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = count > 0, onClick = { onConfirm(choice) }) {
                Text(stringResource(R.string.action_delete), color = if (count > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun EmptyHistory() {
    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        MandalaBackdrop(Modifier.size(200.dp), alpha = 0.12f)
        Text(
            stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
    }
}

@Composable
private fun dayLabel(date: LocalDate, locale: Locale): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
}

@Composable
private fun DayHeader(group: DayGroup, locale: Locale, onDelete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(dayLabel(group.date, locale), style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
            Text(
                pluralStringResource(R.plurals.malas_and_japa, group.records.size, group.records.size, group.records.size * MALA_SIZE),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Sharp.DeleteSweep, contentDescription = stringResource(R.string.delete_day_title), tint = scheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = scheme.outlineVariant)
}

@Composable
private fun RecordRow(record: JapaRecord, locale: Locale, onDelete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val time = remember(record.completedAt, locale) {
        Instant.ofEpochMilli(record.completedAt).atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    }
    val title = Mantras.builtIn(record.mantraKey)?.let { stringResource(it.titleRes) } ?: record.mantraLabel
    SharpCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(time, style = MaterialTheme.typography.titleMedium, color = scheme.primary, modifier = Modifier.width(76.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val d = record.completedAt - record.startedAt
                Text(
                    if (record.startedAt > 0 && d > 0) stringResource(R.string.record_detail, MALA_SIZE, formatDuration(d)) else stringResource(R.string.record_detail_beads, MALA_SIZE),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Sharp.DeleteOutline, contentDescription = stringResource(R.string.delete_entry), tint = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> stringResource(R.string.duration_h_m, h, m)
        m > 0 -> stringResource(R.string.duration_m_s, m, s)
        else -> stringResource(R.string.duration_s, s)
    }
}

/**
 * Seven-day bar chart: one hue (primary), goal as a dashed reference line, value label only on the
 * selected bar (today by default; tap any bar to inspect it). Text uses ink colours, not the bar colour.
 */
@Composable
private fun WeekChart(days: List<DayBar>, goal: Int, locale: Locale) {
    if (days.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    var selected by remember(days) { mutableIntStateOf(days.lastIndex) }
    val maxV = maxOf(days.maxOf { it.malas }, goal, 1)
    val summary = days.joinToString { "${it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)} ${it.malas}" }

    Column(Modifier.semantics { contentDescription = summary }) {
        Box(Modifier.fillMaxWidth().height(24.dp)) {
            val sel = days.getOrNull(selected)
            if (sel != null) {
                Text(
                    pluralStringResource(R.plurals.malas_count, sel.malas, sel.malas) + " · " + dayShort(sel.date, locale),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(days) {
                    detectTapGestures { pos ->
                        val slot = size.width / days.size
                        selected = (pos.x / slot).toInt().coerceIn(0, days.lastIndex)
                    }
                },
        ) {
            val slot = size.width / days.size
            val barW = slot * 0.52f
            val chartH = size.height - 2.dp.toPx()
            // baseline
            drawLine(scheme.outlineVariant, Offset(0f, chartH), Offset(size.width, chartH), 1.dp.toPx())
            // goal reference line
            val gy = chartH - chartH * (goal / maxV.toFloat())
            drawLine(
                scheme.secondary, Offset(0f, gy), Offset(size.width, gy), 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            days.forEachIndexed { i, d ->
                val h = if (d.malas == 0) 0f else (chartH * d.malas / maxV.toFloat()).coerceAtLeast(3.dp.toPx())
                val x = i * slot + (slot - barW) / 2
                val isSel = i == selected
                if (h > 0f) {
                    drawRect(
                        if (isSel) scheme.primary else scheme.primary.copy(alpha = 0.55f),
                        topLeft = Offset(x, chartH - h),
                        size = Size(barW, h),
                    )
                } else if (isSel) {
                    drawRect(scheme.outline, Offset(x, chartH - 2.dp.toPx()), Size(barW, 2.dp.toPx()))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEachIndexed { i, d ->
                Text(
                    d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selected) scheme.onSurface else scheme.onSurfaceVariant,
                    fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun dayShort(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern(if (locale.language in setOf("zh", "ja", "ko")) "M/d" else "d MMM", locale))
