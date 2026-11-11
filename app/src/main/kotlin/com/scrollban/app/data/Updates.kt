package com.scrollban.app.data

import android.content.Context
import com.scrollban.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Aviso de versión nueva. ScrollBan no está en Google Play, así que nadie le actualiza la app a
 * la persona: una vez al día como mucho, la app lee kimycars.com/scrollban/version.json (número
 * de la última versión y enlace de descarga) y, si hay una más nueva, la pestaña Hoy lo dice.
 *
 * Es una lectura: no manda ningún dato (el servidor solo ve la visita, como con cualquier página).
 * Está en la política de privacidad. El fichero se cambia en la web cada vez que se publica un APK.
 */
object Updates {
    private const val ENDPOINT = "https://www.kimycars.com/scrollban/version.json"
    private const val PREFS = "scrollban_updates"
    private const val KEY_CHECKED = "checked_at"
    private const val KEY_CODE = "latest_code"
    private const val KEY_NAME = "latest_name"
    private const val KEY_URL = "latest_url"
    private const val KEY_DISMISSED = "dismissed_code"
    private const val CHECK_EVERY_MS = 24 * 60 * 60_000L
    const val DEFAULT_URL = "https://www.kimycars.com/scrollban/instalar"

    data class Latest(val code: Int, val name: String, val url: String)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** La versión nueva que hay que avisar, o null si la instalada es la última o ya se descartó. */
    fun pending(context: Context): Latest? {
        val p = prefs(context)
        val code = p.getInt(KEY_CODE, 0)
        if (code <= BuildConfig.VERSION_CODE || code == p.getInt(KEY_DISMISSED, 0)) return null
        return Latest(code, p.getString(KEY_NAME, "").orEmpty(), p.getString(KEY_URL, null) ?: DEFAULT_URL)
    }

    /** "Ahora no": no se vuelve a avisar de esta versión, sí de la siguiente. */
    fun dismiss(context: Context, latest: Latest) =
        prefs(context).edit().putInt(KEY_DISMISSED, latest.code).apply()

    /** Pregunta al servidor si ha pasado un día desde la última vez. Los fallos de red se ignoran. */
    suspend fun check(context: Context) = withContext(Dispatchers.IO) {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        if (now - p.getLong(KEY_CHECKED, 0L) < CHECK_EVERY_MS) return@withContext
        runCatching {
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                useCaches = false
            }
            val body = if (conn.responseCode == 200) conn.inputStream.bufferedReader().use { it.readText() } else null
            conn.disconnect()
            val json = JSONObject(body ?: return@runCatching)
            val url = json.optString("url").takeIf { it.startsWith("https://www.kimycars.com/") } ?: DEFAULT_URL
            p.edit()
                .putLong(KEY_CHECKED, now)
                .putInt(KEY_CODE, json.getInt("versionCode"))
                .putString(KEY_NAME, json.optString("versionName"))
                .putString(KEY_URL, url)
                .apply()
        }
    }
}
