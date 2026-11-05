package com.sjpixels.mantramala.overlay

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.sjpixels.mantramala.data.UserSettings

/** Starts/stops the over-other-apps bead. Call from a visible Activity (Android only allows starting it then). */
object FloatingOverlay {
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun shouldRun(context: Context, s: UserSettings): Boolean =
        s.floatingButton && s.floatingOverlay && canDrawOverlays(context)

    fun sync(context: Context, s: UserSettings) {
        val intent = Intent(context, FloatingCounterService::class.java)
        if (shouldRun(context, s)) {
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                // e.g. ForegroundServiceStartNotAllowedException if the app went to background mid-call.
            }
        } else {
            context.stopService(intent)
        }
    }
}
