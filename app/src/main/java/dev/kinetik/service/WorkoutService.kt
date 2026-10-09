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
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.kinetik.MainActivity
import dev.kinetik.R
import dev.kinetik.app
import dev.kinetik.plan.Step
import dev.kinetik.session.Phase
import dev.kinetik.session.SessionEvent
import dev.kinetik.session.SessionState
import dev.kinetik.session.notificationText
import dev.kinetik.session.serviceShouldRun
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Keeps the workout alive with the screen off: foreground notification, wake lock, headphone button. */
class WorkoutService : Service() {
    private val scope = MainScope()
    private lateinit var media: MediaSessionCompat
    private lateinit var wake: PowerManager.WakeLock
    private var lastShown: List<String>? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Workout", NotificationManager.IMPORTANCE_LOW),
        )
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, build(app.session.state.value), type)

        wake = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kinetik:workout")
            .apply { acquire(3 * 60 * 60 * 1000L) }

        media = MediaSessionCompat(this, "kinetik").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = app.session.primaryAction()
                override fun onPause() = app.session.primaryAction()
            })
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                    .build(),
            )
            isActive = true
        }

        scope.launch {
            app.session.state.collect { s ->
                // Finished or ended: drop the notification, wake lock and media session straight away.
                if (serviceShouldRun(s)) show(s!!) else stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DONE -> app.session.send(SessionEvent.Done)
            ACTION_PAUSE -> app.session.send(SessionEvent.TogglePause)
            ACTION_SKIP -> app.session.send(SessionEvent.Skip)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        media.release()
        if (wake.isHeld) wake.release()
        super.onDestroy()
    }

    /** Re-posts only when the visible text or buttons change (about once a second), not on every 100 ms tick. */
    private fun show(s: SessionState) {
        val (title, body) = notificationText(s)
        val key = listOf(title, body, s.phase.name, (s.step is Step.Work).toString())
        if (key == lastShown) return
        lastShown = key
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, build(s))
    }

    private fun build(s: SessionState?): Notification {
        val (title, body) = s?.let(::notificationText) ?: ("Kinetik" to "Starting…")
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_kinetik)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(open)
        if (s != null && s.phase != Phase.FINISHED) {
            if (s.step is Step.Work) {
                b.addAction(0, "Done", action(ACTION_DONE))
            } else {
                b.addAction(0, if (s.phase == Phase.PAUSED) "Resume" else "Pause", action(ACTION_PAUSE))
                b.addAction(0, "Skip", action(ACTION_SKIP))
            }
        }
        return b.build()
    }

    private fun action(a: String): PendingIntent = PendingIntent.getService(
        this, a.hashCode(), Intent(this, WorkoutService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val CHANNEL = "workout"
        private const val NOTIF_ID = 1
        private const val ACTION_DONE = "dev.kinetik.DONE"
        private const val ACTION_PAUSE = "dev.kinetik.PAUSE"
        private const val ACTION_SKIP = "dev.kinetik.SKIP"

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java))

        fun stop(context: Context) {
            context.stopService(Intent(context, WorkoutService::class.java))
        }
    }
}
