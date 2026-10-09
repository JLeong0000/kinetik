package dev.kinetik.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import dev.kinetik.MainActivity
import dev.kinetik.R
import dev.kinetik.app
import dev.kinetik.session.Control
import dev.kinetik.session.Phase
import dev.kinetik.session.SessionState
import dev.kinetik.session.controlsFor
import dev.kinetik.session.notificationText
import dev.kinetik.session.restProgress
import dev.kinetik.session.serviceShouldRun
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the workout alive with the screen off (foreground notification + wake lock) and shows a
 * media-style card so Done / Pause / Skip are on the lock screen. The session has no play/pause
 * actions, so earbud buttons stay with the music app.
 */
class WorkoutService : Service() {
    private val scope = MainScope()
    private lateinit var media: MediaSessionCompat
    private lateinit var wake: PowerManager.WakeLock
    private var lastShown: List<Any?>? = null
    /** A fixed timestamp, so re-posts don't move the notification or its status-bar chip. */
    private val postedAt = System.currentTimeMillis()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Workout", NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        media = MediaSessionCompat(this, "kinetik").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onCustomAction(action: String, extras: android.os.Bundle?) {
                    Control.entries.firstOrNull { it.name == action }?.let { app.session.send(it.event) }
                }
            })
            isActive = true
        }
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, build(app.session.state.value), type)

        wake = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kinetik:workout")
            .apply { acquire(3 * 60 * 60 * 1000L) }

        scope.launch {
            app.session.state.collect { s ->
                // Finished or ended: drop the notification, wake lock and media session straight away.
                if (serviceShouldRun(s)) show(s!!) else stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.removePrefix(ACTION_PREFIX)
            ?.let { name -> Control.entries.firstOrNull { it.name == name } }
            ?.let { app.session.send(it.event) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        media.isActive = false
        media.release()
        if (wake.isHeld) wake.release()
        super.onDestroy()
    }

    /**
     * Re-posts only when the step, pause state, buttons or rest length (−30 s) change, never as time passes:
     * the card's progress bar runs the rest countdown by itself, so the status-bar chip stays put.
     */
    private fun show(s: SessionState) {
        val (title, body) = notificationText(s)
        val controls = controlsFor(s)
        val progress = restProgress(s)
        // Elapsed rest minus time in the step stays constant as time passes and jumps only on −30 s.
        val key = listOf(title, body, progress?.first?.minus(s.stepElapsedMs)) + controls.map { it.name }
        if (key == lastShown) return
        lastShown = key
        media.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, body)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, title)
                .apply { progress?.let { putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it.second) } }
                .build(),
        )
        media.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    if (s.phase == Phase.PAUSED) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING,
                    progress?.first ?: PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f, SystemClock.elapsedRealtime(),
                )
                .apply { controls.forEach { addCustomAction(it.name, it.label, icon(it)) } }
                .build(),
        )
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, build(s))
    }

    private fun build(s: SessionState?): Notification {
        val (title, body) = s?.let(::notificationText) ?: ("Kinetik" to "Starting…")
        val controls = s?.let(::controlsFor).orEmpty()
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_kinetik)
            .setContentTitle(body)
            .setContentText(title)
            .setOngoing(true)
            .setWhen(postedAt)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setStyle(
                MediaStyle()
                    .setMediaSession(media.sessionToken)
                    .setShowActionsInCompactView(*controls.indices.toList().toIntArray()),
            )
        controls.forEach { b.addAction(icon(it), it.label, action(it)) }
        return b.build()
    }

    private fun icon(c: Control): Int = when (c) {
        Control.DONE -> R.drawable.ic_ctl_done
        Control.PAUSE -> R.drawable.ic_ctl_pause
        Control.RESUME -> R.drawable.ic_ctl_resume
        Control.SKIP -> R.drawable.ic_ctl_skip
    }

    private fun action(c: Control): PendingIntent = PendingIntent.getService(
        this, c.ordinal, Intent(this, WorkoutService::class.java).setAction(ACTION_PREFIX + c.name), PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val CHANNEL = "workout"
        private const val NOTIF_ID = 1
        private const val ACTION_PREFIX = "dev.kinetik.control."

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java))

        fun stop(context: Context) {
            context.stopService(Intent(context, WorkoutService::class.java))
        }
    }
}
