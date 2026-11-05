package com.sjpixels.mantramala.overlay

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.LocaleList
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sjpixels.mantramala.MainActivity
import com.sjpixels.mantramala.MantraMalaApp
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.data.MALA_SIZE
import com.sjpixels.mantramala.data.UserSettings
import com.sjpixels.mantramala.ui.theme.Palettes
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Keeps the counter bead on screen over other apps after the user leaves or closes Mantra Mala.
 * Runs as a foreground service (type "specialUse") with an ongoing notification that has a Turn-off action.
 * While an app screen is visible the bead hides, because the in-app bead takes over.
 */
class FloatingCounterService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container get() = (application as MantraMalaApp).container
    private lateinit var windowManager: WindowManager
    private var bead: BeadView? = null
    private var params: WindowManager.LayoutParams? = null
    private var lastSettings = UserSettings()

    /** Notification text in the app's chosen language (services don't get AppCompat's locale on Android ≤ 12). */
    private val localized: Context by lazy {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) {
            this
        } else {
            val config = Configuration(resources.configuration)
            config.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
            createConfigurationContext(config)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(localized.getString(R.string.notif_channel_name))
                .setDescription(localized.getString(R.string.notif_channel_desc))
                .setShowBadge(false)
                .build(),
        )
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification(container.counter.counter.value.count),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
            )
        } catch (_: Exception) {
            stopSelf()
            return
        }

        scope.launch {
            combine(container.settings.settings, container.appVisible) { s, visible -> s to visible }
                .collect { (s, appVisible) ->
                    lastSettings = s
                    if (!FloatingOverlay.shouldRun(this@FloatingCounterService, s)) {
                        stopSelf()
                        return@collect
                    }
                    if (appVisible) removeBead() else showBead(s)
                    bead?.setBeadColor(beadColor(s))
                }
        }
        scope.launch {
            container.counter.counter.collect { state ->
                bead?.setCount(state.count)
                updateNotification(state.count)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TURN_OFF) {
            removeBead()
            // Persist on the app scope so the write survives this service stopping.
            container.appScope.launch { container.settings.setFloatingOverlay(false) }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeBead()
        scope.cancel()
        super.onDestroy()
    }

    private fun beadColor(s: UserSettings): Int =
        if (s.floatingColor != 0) s.floatingColor else Palettes.byId(s.paletteId).primary.toArgb()

    private fun screenSize(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.let { it.width() to it.height() }
        } else {
            resources.displayMetrics.let { it.widthPixels to it.heightPixels }
        }

    private fun showBead(s: UserSettings) {
        val (sw, sh) = screenSize()
        val density = resources.displayMetrics.density
        // Window = bead + room for its shadow on every side.
        val sizePx = ((s.floatingSize + 2 * SHADOW_DP) * density).roundToInt()
        val p = params ?: WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
        p.width = sizePx
        p.height = sizePx
        p.x = (s.floatX * (sw - sizePx)).roundToInt()
        p.y = (s.floatY * (sh - sizePx)).roundToInt()
        params = p

        val existing = bead
        if (existing != null) {
            windowManager.updateViewLayout(existing, p)
            return
        }
        val view = BeadView(this, padPx = SHADOW_DP * density).apply {
            setCount(container.counter.counter.value.count)
            setBeadColor(beadColor(s))
            setOnClickListener { container.counter.increment() }
        }
        attachTouch(view)
        try {
            windowManager.addView(view, p)
            bead = view
        } catch (_: Exception) {
            // Permission revoked between checks; the settings collector will stop the service.
        }
    }

    private fun removeBead() {
        bead?.let { runCatching { windowManager.removeView(it) } }
        bead = null
    }

    /** Tap = count, drag = move, long-press = open the app. */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachTouch(view: View) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        val handler = Handler(Looper.getMainLooper())
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        var longPressed = false
        val longPress = Runnable {
            if (!dragging) {
                longPressed = true
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                openApp()
            }
        }
        view.setOnTouchListener { v, e ->
            val p = params ?: return@setOnTouchListener false
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; startX = p.x; startY = p.y
                    dragging = false; longPressed = false
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start()
                    handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) {
                        dragging = true
                        handler.removeCallbacks(longPress)
                    }
                    if (dragging) {
                        val (sw, sh) = screenSize()
                        p.x = (startX + dx).roundToInt().coerceIn(0, sw - p.width)
                        p.y = (startY + dy).roundToInt().coerceIn(0, sh - p.height)
                        runCatching { windowManager.updateViewLayout(v, p) }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPress)
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    if (dragging) {
                        val (sw, sh) = screenSize()
                        val fx = p.x.toFloat() / (sw - p.width).coerceAtLeast(1)
                        val fy = p.y.toFloat() / (sh - p.height).coerceAtLeast(1)
                        container.appScope.launch { container.settings.setFloatPosition(fx, fy) }
                    } else if (!longPressed) {
                        v.performClick()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
            }
            true
        }
    }

    private fun openApp() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
        )
    }

    private fun notification(count: Int): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val off = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingCounterService::class.java).setAction(ACTION_TURN_OFF),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(beadColor(lastSettings))
            .setContentTitle(localized.getString(R.string.notif_title))
            .setContentText(localized.getString(R.string.notif_text, count, MALA_SIZE))
            .setContentIntent(open)
            .addAction(0, localized.getString(R.string.notif_action_off), off)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(count: Int) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification(count))
    }

    companion object {
        private const val CHANNEL_ID = "floating_counter"
        private const val NOTIFICATION_ID = 108
        private const val ACTION_TURN_OFF = "com.sjpixels.mantramala.action.TURN_OFF_FLOATING"
        private const val SHADOW_DP = 6
    }
}
