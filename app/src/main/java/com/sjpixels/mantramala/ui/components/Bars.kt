package com.sjpixels.mantramala.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.MenuBook
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.DarkMode
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.material.icons.sharp.History
import androidx.compose.material.icons.sharp.LightMode
import androidx.compose.material.icons.sharp.Palette
import androidx.compose.material.icons.sharp.SelfImprovement
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.ui.Tab

/** True when the app window is shorter than 480dp, e.g. a phone in landscape. */
@Composable
fun isShortWindow(): Boolean {
    val heightPx = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height
    return with(androidx.compose.ui.platform.LocalDensity.current) { heightPx.toDp() } < 480.dp
}

@Composable
fun AppTopBar(dark: Boolean, onToggleDark: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val compact = isShortWindow()
    Surface(color = scheme.background) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(if (compact) 50.dp else 60.dp).padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OmMark(size = 34.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleDark) {
                    AnimatedContent(
                        targetState = dark,
                        transitionSpec = { (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut()) },
                        label = "daynight",
                    ) { isDark ->
                        Icon(
                            if (isDark) Icons.Sharp.LightMode else Icons.Sharp.DarkMode,
                            contentDescription = stringResource(R.string.toggle_day_night),
                            tint = scheme.secondary,
                        )
                    }
                }
                LanguageButton()
            }
            HorizontalDivider(color = scheme.outlineVariant)
        }
    }
}

@Composable
fun LanguageButton(modifier: Modifier = Modifier, expandedWidth: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    val current = remember { AppLanguages.current() }
    val scheme = MaterialTheme.colorScheme
    val description = stringResource(R.string.change_language)
    Box(modifier) {
        Surface(
            onClick = { open = true },
            shape = CutCornerShape(6.dp),
            color = scheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            modifier = if (expandedWidth) Modifier.fillMaxWidth() else Modifier,
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(current.flag, fontSize = 20.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (expandedWidth) current.nativeName else current.tag.substringBefore('-').uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurface,
                    modifier = if (expandedWidth) Modifier.weight(1f) else Modifier,
                )
                Icon(Icons.Sharp.ExpandMore, contentDescription = description, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = MaterialTheme.shapes.medium) {
            AppLanguages.all.forEach { lang ->
                DropdownMenuItem(
                    leadingIcon = { Text(lang.flag, fontSize = 22.sp) },
                    text = { Text(lang.nativeName, fontWeight = if (lang == current) FontWeight.Bold else FontWeight.Normal) },
                    trailingIcon = if (lang == current) {
                        { Icon(Icons.Sharp.Check, contentDescription = null, tint = scheme.primary) }
                    } else null,
                    onClick = {
                        open = false
                        if (lang != current) AppLanguages.apply(lang)
                    },
                )
            }
        }
    }
}

private data class TabSpec(val tab: Tab, val icon: ImageVector, val label: Int)

private val tabs = listOf(
    TabSpec(Tab.JAPA, Icons.Sharp.SelfImprovement, R.string.tab_japa),
    TabSpec(Tab.MANTRAS, Icons.AutoMirrored.Sharp.MenuBook, R.string.tab_mantras),
    TabSpec(Tab.HISTORY, Icons.Sharp.History, R.string.tab_history),
    TabSpec(Tab.THEMES, Icons.Sharp.Palette, R.string.tab_themes),
)

@Composable
fun AppBottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    // Short windows (phone in landscape): icon-only bar to leave room for the counter.
    val compact = isShortWindow()
    Surface(color = scheme.surfaceContainer) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = scheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().height(if (compact) 46.dp else 64.dp).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { spec ->
                    val isSel = spec.tab == selected
                    val a by animateFloatAsState(if (isSel) 1f else 0f, label = "tab")
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(role = Role.Tab, onClick = { onSelect(spec.tab) })
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .width(56.dp)
                                .height(30.dp)
                                .background(scheme.primary.copy(alpha = 0.16f * a), CutCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                spec.icon,
                                // With labels hidden (compact), the icon itself names the tab for TalkBack.
                                contentDescription = if (compact) stringResource(spec.label) else null,
                                tint = if (isSel) scheme.primary else scheme.onSurfaceVariant,
                            )
                        }
                        if (!compact) Spacer(Modifier.height(3.dp))
                        if (!compact) Text(
                            stringResource(spec.label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) scheme.primary else scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(0.75f + 0.25f * a),
                        )
                    }
                }
            }
            BuiltByLink(Modifier.fillMaxWidth().padding(bottom = if (compact) 0.dp else 2.dp))
        }
    }
}
