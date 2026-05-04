package com.screenpact.app.services

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
import com.screenpact.app.MainActivity
import com.screenpact.app.R
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.util.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        while (true) {
            try {
                if (UsageStatsHelper.hasUsageAccess(this)) {
                    val now = System.currentTimeMillis()
                    db.unlockGrantDao().cleanExpired(now)

                    val current = UsageStatsHelper.currentForegroundPackage(this)
                    var shouldBlock = false
                    if (current != null && current != packageName) {
                        val limit = db.appLimitDao().get(current)
                        if (limit != null && limit.enabled) {
                            val grant = db.unlockGrantDao().get(current)
                            val grantActive = grant != null && grant.expiresAt > now
                            if (!grantActive) {
                                val totals = UsageStatsHelper.foregroundTimeTodayMs(this)
                                val usedMs = totals[current] ?: 0L
                                if (usedMs >= limit.dailyLimitMinutes * 60_000L) {
                                    shouldBlock = true
                                    OverlayService.show(this, current, limit.appName, usedMs, limit.dailyLimitMinutes)
                                }
                            }
                        }
                    }
                    if (!shouldBlock) {
                        OverlayService.hide(this)
                    }
                }
            } catch (_: Throwable) { /* swallow & continue */ }
            delay(POLL_INTERVAL_MS)
        }
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
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setOngoing(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 5_000L
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
