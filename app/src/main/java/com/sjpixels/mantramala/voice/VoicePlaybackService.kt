package com.sjpixels.mantramala.voice

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.LocaleList
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps voice playback alive with the screen off or while other apps are in use, and shows
 * the voice count with Play/Pause and Stop in a notification. Ends as soon as playback is stopped.
 */
class VoicePlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player: VoicePlayer get() = (application as MantraMalaApp).container.voicePlayer

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

    /** Headphones unplugged / Bluetooth disconnected: pause instead of playing out loud. */
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) player.pause()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(localized.getString(R.string.voice_channel_name))
                .setShowBadge(false)
                .build(),
        )
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
        } catch (_: Exception) {
            player.stop()
            stopSelf()
            return
        }
        ContextCompat.registerReceiver(
            this, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scope.launch {
            combine(player.playback, player.counter) { p, c -> p to c }.collect { (p, _) ->
                if (p.status == VoiceStatus.IDLE) {
                    stopSelf()
                } else {
                    updateNotification()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> player.toggle()
            ACTION_STOP -> player.stop()
        }
        if (player.playback.value.status == VoiceStatus.IDLE) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        runCatching { unregisterReceiver(noisyReceiver) }
        scope.cancel()
        // Never keep playing without the foreground service.
        if (player.playback.value.status != VoiceStatus.IDLE) player.stop()
        super.onDestroy()
    }

    private fun notification(): Notification {
        val p = player.playback.value
        val c = player.counter.value
        val open = PendingIntent.getActivity(
            this, 10,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(a: String, code: Int) = PendingIntent.getService(
            this, code, Intent(this, VoicePlaybackService::class.java).setAction(a),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val playing = p.status == VoiceStatus.PLAYING
        val text = if (p.pausedAfterMala) localized.getString(R.string.voice_mala_done)
        else localized.getString(R.string.voice_count, c.count, MALA_SIZE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(p.sample?.name ?: localized.getString(R.string.voice_channel_name))
            .setContentText(text)
            .setContentIntent(open)
            .addAction(0, localized.getString(if (playing) R.string.voice_pause else R.string.voice_play), action(ACTION_TOGGLE, 11))
            .addAction(0, localized.getString(R.string.voice_stop), action(ACTION_STOP, 12))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification() {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification())
    }

    companion object {
        /** True between onCreate and onDestroy, so the player knows whether to start the service. */
        @Volatile var running = false
            private set
        private const val CHANNEL_ID = "voice_playback"
        private const val NOTIFICATION_ID = 109
        private const val ACTION_TOGGLE = "com.sjpixels.mantramala.action.VOICE_TOGGLE"
        private const val ACTION_STOP = "com.sjpixels.mantramala.action.VOICE_STOP"
    }
}
