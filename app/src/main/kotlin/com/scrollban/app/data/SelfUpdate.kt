package com.scrollban.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.pm.PackageInfoCompat
import com.scrollban.app.BuildConfig
import com.scrollban.app.MainActivity
import com.scrollban.app.R
import com.scrollban.app.services.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Actualización dentro de la app (lo más parecido al OTA de Vestiarium que permite Android en una
 * app nativa): descarga el APK de kimycars.com con su barra de progreso y se lo pasa al instalador
 * del sistema, que pide confirmar con un toque. La primera vez Android pide además permitir que
 * ScrollBan instale apps. Al terminar, Android cierra la app; una notificación ofrece volver a
 * abrirla (BootReceiver, al recibir MY_PACKAGE_REPLACED).
 *
 * Solo instala ScrollBan: se comprueba que el APK sea este mismo paquete y más nuevo, y Android
 * rechaza cualquier APK que no esté firmado con la misma clave. Si algo falla, la tarjeta ofrece
 * descargarla desde la web como antes.
 */
object SelfUpdate {
    sealed interface State {
        data object Idle : State
        data class Downloading(val fraction: Float) : State
        data object NeedsPermission : State
        data object Installing : State
        data object Failed : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private const val PREFS = "scrollban_updates"
    private const val KEY_INSTALLING = "installing_name"
    private const val CHANNEL = "scrollban_updates"
    private const val NOTIF_ID = 1002

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Ajuste de Android "Permitir instalar apps de esta fuente" para ScrollBan. */
    fun openInstallPermission(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun reset() { _state.value = State.Idle }

    internal fun failed() { _state.value = State.Failed }

    suspend fun start(context: Context, latest: Updates.Latest) {
        val app = context.applicationContext
        if (!canInstall(app)) { _state.value = State.NeedsPermission; return }
        _state.value = State.Downloading(0f)
        val file = withContext(Dispatchers.IO) { runCatching { download(app, latest.apk) }.getOrNull() }
        if (file == null || !isNewerSelf(app, file)) { _state.value = State.Failed; return }
        _state.value = State.Installing
        val ok = withContext(Dispatchers.IO) { runCatching { install(app, file, latest.name) }.isSuccess }
        if (!ok) _state.value = State.Failed
    }

    private fun download(context: Context, url: String): File {
        val dir = File(context.cacheDir, "update").apply { mkdirs() }
        val file = File(dir, "scrollban.apk")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
        }
        try {
            check(conn.responseCode == 200)
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) _state.value = State.Downloading((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return file
    }

    /** Que sea ScrollBan (este mismo paquete) y más nueva que la instalada. La firma la comprueba Android. */
    private fun isNewerSelf(context: Context, file: File): Boolean {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(file.path, 0) ?: return false
        return info.packageName == context.packageName &&
            PackageInfoCompat.getLongVersionCode(info) > BuildConfig.VERSION_CODE
    }

    private fun install(context: Context, file: File, name: String) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(file.length())
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("scrollban.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_INSTALLING, name).apply()
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val callback = PendingIntent.getBroadcast(context, id, Intent(context, UpdateReceiver::class.java), flags)
            session.commit(callback.intentSender)
        }
    }

    /**
     * Tras actualizarse: Android ha cerrado la app. Si la actualización salió de aquí, una
     * notificación invita a abrirla otra vez (Android no deja que se abra sola).
     */
    fun notifyUpdated(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_INSTALLING, null) ?: return
        prefs.edit().remove(KEY_INSTALLING).apply()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Actualizaciones", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ScrollBan está actualizada")
            .setContentText(if (name.isNotBlank()) "Ya tienes la versión $name. Toca para abrirla." else "Toca para abrirla.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(NOTIF_ID, notification) }
    }
}
