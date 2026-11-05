package com.sjpixels.mantramala.ui.counter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.KeyboardArrowRight
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.ExpandLess
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.sharp.Grain
import androidx.compose.material.icons.sharp.LocalFireDepartment
import androidx.compose.material.icons.sharp.Remove
import androidx.compose.material.icons.sharp.RestartAlt
import androidx.compose.material.icons.sharp.Spa
import androidx.compose.material.icons.sharp.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.data.CounterStyle
import com.sjpixels.mantramala.data.MALA_SIZE
import com.sjpixels.mantramala.ui.MainViewModel
import com.sjpixels.mantramala.ui.MantraOption
import com.sjpixels.mantramala.ui.components.Diamond
import com.sjpixels.mantramala.ui.components.MandalaBackdrop
import com.sjpixels.mantramala.ui.components.SharpCard
import com.sjpixels.mantramala.ui.mantras.mantraTitle
import com.sjpixels.mantramala.ui.voice.VoiceBar
import com.sjpixels.mantramala.ui.theme.Cinzel
import com.sjpixels.mantramala.ui.theme.Devanagari
import kotlinx.coroutines.launch
import java.text.NumberFormat

@Composable
fun CounterScreen(vm: MainViewModel, onOpenMantras: () -> Unit) {
    val counter by vm.counter.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val mantra by vm.selectedMantra.collectAsStateWithLifecycle()
    val completions by vm.completions.collectAsStateWithLifecycle()

    var confirmReset by rememberSaveable { mutableStateOf(false) }

    // Animated bead progress. On completion, sweep to 108 before settling on the new count.
    val progress = remember { Animatable(counter.count.toFloat()) }
    var seenCompletions by remember { mutableIntStateOf(completions) }
    val burst = remember { Animatable(0f) }
    var showBanner by remember { mutableStateOf(false) }
    // Completions that happened before this screen was (re)created must not replay the banner.
    val initialCompletions = remember { completions }

    LaunchedEffect(counter.count, completions) {
        if (completions != seenCompletions) {
            seenCompletions = completions
            progress.animateTo(MALA_SIZE.toFloat(), tween(260))
            progress.snapTo(0f)
        }
        progress.animateTo(counter.count.toFloat(), spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(completions) {
        if (completions <= initialCompletions) return@LaunchedEffect
        showBanner = true
        launch { burst.snapTo(0f); burst.animateTo(1f, tween(1400)) }
        kotlinx.coroutines.delay(2600)
        showBanner = false
    }

    val dial: @Composable (Modifier) -> Unit = { mod ->
        Dial(
            count = counter.count,
            progress = progress.value,
            burst = burst.value,
            style = settings.counterStyle,
            malaNumber = stats.todayMalas + 1,
            showBanner = showBanner,
            todayMalas = stats.todayMalas,
            onTap = vm::increment,
            modifier = mod,
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight && maxWidth >= 560.dp
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    DialBox(dial)
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                ) {
                    MantraHeader(mantra, onOpenMantras)
                    VoiceBar(vm, mantra?.let { mantraTitle(it) } ?: "")
                    Controls(onMinus = vm::decrement, onReset = { confirmReset = true }, onPlus = vm::increment, resetEnabled = counter.count > 0)
                    StatsRow(stats.todayMalas, settings.dailyGoal, stats.currentStreak, stats.totalMalas, stats.totalJapa + counter.count)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                MantraHeader(mantra, onOpenMantras)
                Spacer(Modifier.height(6.dp))
                VoiceBar(vm, mantra?.let { mantraTitle(it) } ?: "")
                Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    DialBox(dial)
                }
                Controls(onMinus = vm::decrement, onReset = { confirmReset = true }, onPlus = vm::increment, resetEnabled = counter.count > 0)
                Spacer(Modifier.height(8.dp))
                StatsRow(stats.todayMalas, settings.dailyGoal, stats.currentStreak, stats.totalMalas, stats.totalJapa + counter.count)
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_round_title)) },
            text = { Text(stringResource(R.string.reset_round_message, counter.count)) },
            confirmButton = {
                TextButton(onClick = { vm.resetRound(); confirmReset = false }) { Text(stringResource(R.string.action_reset)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun DialBox(dial: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight).coerceAtMost(420.dp)
        dial(Modifier.size(side))
    }
}

@Composable
private fun Dial(
    count: Int,
    progress: Float,
    burst: Float,
    style: CounterStyle,
    malaNumber: Int,
    showBanner: Boolean,
    todayMalas: Int,
    onTap: () -> Unit,
    modifier: Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "press")
    val scheme = MaterialTheme.colorScheme
    val tapLabel = stringResource(R.string.tap_to_chant)
    val a11y = stringResource(R.string.a11y_counter, count, MALA_SIZE)

    BoxWithConstraints(
        modifier
            .scale(scale)
            .semantics { contentDescription = a11y }
            .clickable(interactionSource = interaction, indication = null, onClickLabel = tapLabel, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        val side = maxWidth
        MandalaBackdrop(Modifier.size(side * 1.0f), alpha = 0.06f)
        DialArt(progress = progress, style = style, burst = burst, modifier = Modifier.fillMaxSize())

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = showBanner,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.8f)) },
                label = "banner",
            ) { banner ->
                if (banner) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🙏", fontSize = (side.value * 0.12f).sp)
                        Text(
                            stringResource(R.string.mala_complete),
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            pluralStringResource(R.plurals.malas_today, todayMalas, todayMalas),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(
                            targetState = count,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                                } else {
                                    (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
                                }
                            },
                            label = "count",
                        ) { value ->
                            Text(
                                value.toString(),
                                fontFamily = Cinzel,
                                fontWeight = FontWeight.Bold,
                                fontSize = (side.value * 0.2f).sp,
                                color = scheme.onBackground,
                            )
                        }
                        Text(
                            "/ $MALA_SIZE",
                            fontFamily = Cinzel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (side.value * 0.045f).sp,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(side * 0.025f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Diamond(scheme.secondary, size = 6.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (count == 0 && todayMalas == 0) tapLabel.uppercase()
                                else stringResource(R.string.mala_number, malaNumber).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp),
                                color = scheme.secondary,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(6.dp))
                            Diamond(scheme.secondary, size = 6.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The mantra being chanted. Shows the full Devanagari text (up to 3 lines); tap it, or the arrow,
 * to expand to the complete text with transliteration and meaning. "Change" opens the library.
 */
@Composable
private fun MantraHeader(mantra: MantraOption?, onChange: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(false) }
    val meaning = mantra?.builtInMeaningRes?.let { stringResource(it) } ?: mantra?.customMeaning?.takeIf { it.isNotBlank() }
    var overflows by remember(mantra?.key) { mutableStateOf(false) }
    val canExpand = overflows || mantra?.transliteration != null || meaning != null
    val toggleLabel = stringResource(if (expanded) R.string.mantra_show_less else R.string.mantra_show_more)

    SharpCard(modifier = Modifier.fillMaxWidth().animateContentSize(), contentPadding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.now_chanting).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.secondary,
                )
                Text(
                    mantra?.let { mantraTitle(it) } ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier.clickable(onClick = onChange).padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.action_change), style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                Icon(Icons.AutoMirrored.Sharp.KeyboardArrowRight, contentDescription = null, tint = scheme.primary)
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = canExpand, onClickLabel = toggleLabel) { expanded = !expanded },
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .then(if (expanded) Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()) else Modifier),
            ) {
                Text(
                    mantra?.text ?: "",
                    fontFamily = Devanagari,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = scheme.primary,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
                )
                if (expanded) {
                    mantra?.transliteration?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = scheme.onSurfaceVariant)
                    }
                    meaning?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface.copy(alpha = 0.85f))
                    }
                }
            }
            if (canExpand) {
                Icon(
                    if (expanded) Icons.Sharp.ExpandLess else Icons.Sharp.ExpandMore,
                    contentDescription = toggleLabel,
                    tint = scheme.primary,
                )
            }
        }
    }
}

@Composable
private fun Controls(onMinus: () -> Unit, onReset: () -> Unit, onPlus: () -> Unit, resetEnabled: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SharpButton(Icons.Sharp.Remove, stringResource(R.string.action_decrease), onMinus, filled = false, modifier = Modifier.weight(1f), height = 36.dp)
        SharpButton(
            Icons.Sharp.RestartAlt, stringResource(R.string.action_reset_round), onReset,
            filled = false, showLabel = true, enabled = resetEnabled, modifier = Modifier.weight(1.4f), height = 36.dp,
        )
        SharpButton(Icons.Sharp.Add, stringResource(R.string.action_increase), onPlus, filled = true, modifier = Modifier.weight(1f), height = 36.dp)
    }
}

@Composable
private fun SharpButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    filled: Boolean,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
        color = if (filled) scheme.primary else scheme.surfaceContainerHigh,
        contentColor = if (filled) scheme.onPrimary else scheme.onSurface,
        border = if (filled) null else BorderStroke(1.dp, scheme.outlineVariant),
        modifier = modifier.height(height),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val alpha = if (enabled) 1f else 0.38f
            Icon(icon, contentDescription = if (showLabel) null else label, tint = androidx.compose.material3.LocalContentColor.current.copy(alpha = alpha))
            if (showLabel) {
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = androidx.compose.material3.LocalContentColor.current.copy(alpha = alpha),
                )
            }
        }
    }
}

@Composable
private fun StatsRow(today: Int, goal: Int, streak: Int, totalMalas: Int, totalJapa: Long) {
    val locale = LocalConfiguration.current.locales[0] ?: java.util.Locale.ROOT
    val nf = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(
            Icons.Sharp.WbSunny, stringResource(R.string.stat_today), "$today/$goal",
            progress = (today.toFloat() / goal).coerceIn(0f, 1f), modifier = Modifier.weight(1f), compact = true,
        )
        StatTile(
            Icons.Sharp.LocalFireDepartment, stringResource(R.string.stat_streak),
            pluralStringResource(R.plurals.days_short, streak, streak), modifier = Modifier.weight(1f), compact = true,
        )
        StatTile(Icons.Sharp.Spa, stringResource(R.string.stat_malas), nf.format(totalMalas), modifier = Modifier.weight(1f), compact = true)
        StatTile(Icons.Sharp.Grain, stringResource(R.string.stat_japa), compact(totalJapa, nf, locale), modifier = Modifier.weight(1f), compact = true)
    }
}

private fun compact(n: Long, nf: NumberFormat, locale: java.util.Locale): String = when {
    n >= 1_000_000 -> String.format(locale, "%.1fM", n / 1_000_000.0)
    n >= 100_000 -> String.format(locale, "%.0fK", n / 1_000.0)
    else -> nf.format(n)
}

@Composable
fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, progress: Float? = null, compact: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    SharpCard(modifier = modifier, contentPadding = if (compact) 7.dp else 10.dp) {
        if (compact) {
            // Icon beside the value: one row shorter, for the Japa screen.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Icon(icon, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (progress != null) {
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = scheme.primary,
                trackColor = scheme.outlineVariant,
                strokeCap = StrokeCap.Butt,
                drawStopIndicator = {},
            )
        }
    }
}
