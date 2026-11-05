package com.sjpixels.mantramala

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sjpixels.mantramala.overlay.FloatingOverlay
import kotlinx.coroutines.launch
import com.sjpixels.mantramala.data.ThemeMode
import com.sjpixels.mantramala.ui.AppRoot
import com.sjpixels.mantramala.ui.MainViewModel
import com.sjpixels.mantramala.ui.Tab
import com.sjpixels.mantramala.ui.theme.MantraMalaTheme
import com.sjpixels.mantramala.ui.theme.Palettes

/** AppCompatActivity (not ComponentActivity) so AppCompat can apply the per-app language. */
class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()
    private val container get() = (application as MantraMalaApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !vm.ready.value }
        enableEdgeToEdge()

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            MantraMalaTheme(palette = Palettes.byId(settings.paletteId), dark = dark) {
                AppRoot(vm, dark)
            }
        }

        // The over-other-apps bead may only be started while a screen is visible, so keep it in sync here.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.settings.settings.collect { FloatingOverlay.sync(this@MainActivity, it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.appVisible.value = true
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) container.appVisible.value = false
    }

    override fun onResume() {
        super.onResume()
        vm.refreshDay()
    }

    /** Optional: count with either volume key while the Japa tab is open (eyes-closed chanting). */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isVolume = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolume && vm.settings.value.volumeKeys && vm.tab.value == Tab.JAPA) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) vm.increment()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
