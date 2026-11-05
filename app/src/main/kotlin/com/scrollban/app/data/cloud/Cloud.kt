package com.scrollban.app.data.cloud

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Conexión con el servidor de Supabase: identidad anónima (sin correo ni contraseña) y llamadas
 * a las funciones de `supabase/schema.sql`. Es el único sitio de la app que habla con Supabase.
 *
 * La URL y la clave publishable son públicas: van dentro de la app a propósito. La clave secreta
 * del proyecto no se usa nunca aquí.
 */
object Cloud {

    private const val URL_BASE = "https://rsdtrjecytejtlihulof.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_T5yht-FM0N4WEHqkL5ofew_mAMmjWh3"
    private const val PREFS = "scrollban_cloud"
    private const val TIMEOUT_MS = 15_000

    class CloudException(val code: Int, message: String) : Exception(message)

    private val sessionLock = Mutex()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** True si esta instalación ya tiene identidad en el servidor. */
    fun hasSession(context: Context): Boolean = prefs(context).getString("refresh", null) != null

    /** Olvida la identidad local (tras borrar los datos del servidor). */
    fun forget(context: Context) = prefs(context).edit().clear().apply()

    /** Llama a una función del servidor y devuelve su respuesta en texto. */
    suspend fun rpc(context: Context, name: String, body: JSONObject = JSONObject()): String {
        val token = accessToken(context)
        return try {
            request("/rest/v1/rpc/$name", body, token)
        } catch (e: CloudException) {
            if (e.code != 401) throw e
            // El token caducó antes de lo previsto: se renueva una vez y se reintenta.
            request("/rest/v1/rpc/$name", body, accessToken(context, forceRefresh = true))
        }
    }

    /** Token válido: lo renueva si le queda poco, o crea la identidad anónima la primera vez. */
    private suspend fun accessToken(context: Context, forceRefresh: Boolean = false): String = sessionLock.withLock {
        val p = prefs(context)
        val access = p.getString("access", null)
        val refresh = p.getString("refresh", null)
        val expiresAt = p.getLong("expires_at", 0L)
        if (!forceRefresh && access != null && System.currentTimeMillis() < expiresAt - 60_000L) return@withLock access

        val session = if (refresh != null) {
            runCatching { request("/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refresh), null) }
                .getOrElse { e ->
                    // Si la sesión ya no existe en el servidor, se empieza una nueva.
                    if (e is CloudException && e.code in 400..499) signUp() else throw e
                }
        } else {
            signUp()
        }
        val json = JSONObject(session)
        val newAccess = json.getString("access_token")
        p.edit()
            .putString("access", newAccess)
            .putString("refresh", json.getString("refresh_token"))
            .putLong("expires_at", System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L)
            .apply()
        newAccess
    }

    private suspend fun signUp(): String = request("/auth/v1/signup", JSONObject(), null)

    private suspend fun request(path: String, body: JSONObject, bearer: String?): String = withContext(Dispatchers.IO) {
        val conn = (URL(URL_BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", PUBLISHABLE_KEY)
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw CloudException(code, text.take(300))
            text
        } finally {
            conn.disconnect()
        }
    }
}
