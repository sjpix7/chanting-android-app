package com.sjpixels.mantramala.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.data.ThemeMode
import com.sjpixels.mantramala.ui.components.AppBottomBar
import com.sjpixels.mantramala.ui.components.AppTopBar
import com.sjpixels.mantramala.ui.components.FloatingBead
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.sjpixels.mantramala.ui.counter.CounterScreen
import com.sjpixels.mantramala.ui.history.HistoryScreen
import com.sjpixels.mantramala.ui.mantras.MantraScreen
import com.sjpixels.mantramala.ui.settings.ThemesScreen

@Composable
fun AppRoot(vm: MainViewModel, dark: Boolean) {
    val tab by vm.tab.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val deletedMessage by rememberUpdatedState(stringResource(R.string.entry_deleted))
    val undoLabel by rememberUpdatedState(stringResource(R.string.action_undo))

    BackHandler(enabled = tab != Tab.JAPA) { vm.selectTab(Tab.JAPA) }

    // Keep the screen awake while chanting, if enabled.
    val view = LocalView.current
    val keepOn = settings.keepScreenOn && tab == Tab.JAPA
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    val resources = LocalResources.current
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            if (e is UiEvent.RecordDeleted) {
                snackbar.currentSnackbarData?.dismiss()
                val r = snackbar.showSnackbar(
                    message = deletedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
                if (r == SnackbarResult.ActionPerformed) vm.restoreRecord(e.record)
            } else if (e is UiEvent.Message) {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(message = resources.getString(e.res), duration = SnackbarDuration.Long)
            }
        }
    }

    val counter by vm.counter.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(dark = dark, onToggleDark = {
                vm.setThemeMode(if (dark) ThemeMode.LIGHT else ThemeMode.DARK)
            })
        },
        bottomBar = { AppBottomBar(selected = tab, onSelect = vm::selectTab) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "tabs",
            ) { t ->
                when (t) {
                    Tab.JAPA -> CounterScreen(vm, onOpenMantras = { vm.selectTab(Tab.MANTRAS) })
                    Tab.MANTRAS -> MantraScreen(vm, onChosen = { vm.selectTab(Tab.JAPA) })
                    Tab.HISTORY -> HistoryScreen(vm)
                    Tab.THEMES -> ThemesScreen(vm)
                }
            }
        }
    }
    if (settings.floatingButton) {
        FloatingBead(
            count = counter.count,
            color = if (settings.floatingColor != 0) Color(settings.floatingColor) else MaterialTheme.colorScheme.primary,
            position = Offset(settings.floatX, settings.floatY),
            onPositionChange = { vm.setFloatPosition(it.x, it.y) },
            onTap = vm::increment,
            beadSize = settings.floatingSize.dp,
        )
    }
    }
}
