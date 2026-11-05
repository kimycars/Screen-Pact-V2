package com.scrollban.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.scrollban.app.MainActivity
import com.scrollban.app.R
import com.scrollban.app.data.db.AppDatabase
import com.scrollban.app.util.TamperGuard
import com.scrollban.app.util.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.scrollban.app.data.History
import com.scrollban.app.data.TimeWon
import com.scrollban.app.data.cloud.Together
import com.scrollban.app.data.Pro
import com.scrollban.app.data.Schedules

/**
 * Servicio en primer plano que cada N segundos:
 *  1. Mira qué app está en primer plano.
 *  2. Mira el tiempo total de hoy en esa app.
 *  3. Si supera el límite y no hay grace period, pide al OverlayService que muestre la pantalla roja.
 */
class UsageMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildNotification(), foregroundType())
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
        loop = scope.launch { runLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        loop?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runLoop() {
        val db = AppDatabase.get(applicationContext)
        var lastHistoryAt = 0L
        while (true) {
            try {
                val hasFriends = db.friendDao().getAll().isNotEmpty()
                // Se evalúa siempre, también sin permisos, para registrar latido y trampas.
                val tamper = TamperGuard.evaluate(this, hasFriends)
                var shouldBlock = false
                var blockedPkg: String? = null
                if (UsageStatsHelper.hasUsageAccess(this)) {
                    val now = System.currentTimeMillis()
                    db.unlockGrantDao().cleanExpired(now)

                    val snap = UsageStatsHelper.snapshot(this)
                    val current = snap.foreground
                    if (now - lastHistoryAt >= HISTORY_EVERY_MS) {
                        val enabled = db.appLimitDao().getEnabled()
                        TimeWon.ensureBaselines(this, db, enabled)
                        History.recordToday(db, enabled, snap.todayMs)
                        lastHistoryAt = now
                        // Aparte, para que la red nunca retrase un bloqueo.
                        scope.launch { Together.maybeSync(applicationContext) }
                    }
                    if (current != null && current != packageName) {
                        val limit = db.appLimitDao().get(current)
                        if (limit != null && limit.enabled) {
                            val usedMs = snap.todayMs[current] ?: 0L
                            if (tamper != null) {
                                // Con trampa activa se bloquean todas las apps limitadas, sin gracia.
                                shouldBlock = true
                                blockedPkg = current
                                OverlayService.show(this, current, limit.appName, usedMs, limit.dailyLimitMinutes, tamper = tamper)
                            } else {
                                val grant = db.unlockGrantDao().get(current)
                                val grantActive = grant != null && grant.expiresAt > now
                                // Horario de cierre (Pro): bloquea aunque quede tiempo.
                                val closedBySchedule = Pro.isPro(this) && Schedules.isClosedNow(limit, now)
                                if (!grantActive && (closedBySchedule || usedMs >= limit.dailyLimitMinutes * 60_000L)) {
                                    shouldBlock = true
                                    blockedPkg = current
                                    val schedule = if (closedBySchedule)
                                        Schedules.describe(limit.scheduleStart!!, limit.scheduleEnd!!) else null
                                    OverlayService.show(this, current, limit.appName, usedMs, limit.dailyLimitMinutes, schedule = schedule)
                                }
                            }
                        }
                    }
                }
                if (!shouldBlock) {
                    OverlayService.hide(this)
                } else if (blockedPkg != null && watchForExit(blockedPkg)) {
                    // Ya se ha ido de la app bloqueada: no esperar a la siguiente vuelta.
                    OverlayService.hide(this)
                    continue
                }
            } catch (_: Throwable) { /* swallow & continue */ }
            delay(POLL_INTERVAL_MS)
        }
    }

    /**
     * Mientras la pantalla de bloqueo está visible, mira cada poco si el usuario ha salido de la
     * app (gesto de inicio, otra app, pantalla apagada) para quitarla al momento. Devuelve true
     * si ha salido antes de que toque la siguiente vuelta normal del bucle.
     */
    private suspend fun watchForExit(blockedPkg: String): Boolean {
        var since = System.currentTimeMillis() - EXIT_CHECK_MS
        val until = since + POLL_INTERVAL_MS
        while (System.currentTimeMillis() < until) {
            delay(EXIT_CHECK_MS)
            val now = System.currentTimeMillis()
            if (UsageStatsHelper.leftApp(this, blockedPkg, since)) return true
            since = now - EXIT_CHECK_MS
        }
        return false
    }

    private fun foregroundType(): Int = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->   // API 34+
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->                  // API 29-33
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        else -> 0
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.monitor_notification_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun buildNotification(): android.app.Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setOngoing(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 2_000L
        private const val EXIT_CHECK_MS = 300L
        private const val HISTORY_EVERY_MS = 60_000L
        private const val NOTIF_ID = 1001
        private const val CHANNEL_ID = "screenpact_monitor"

        fun start(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageMonitorService::class.java))
        }
    }
}
