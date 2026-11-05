package com.scrollban.app.data

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.scrollban.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeUnit

/**
 * ScrollBan Pro. Única fuente de verdad para saber si la persona tiene Pro.
 *
 * Cómo funciona: al pagar con Stripe, el servidor de kimycars.com firma una licencia con su clave
 * privada (ECDSA P-256) y se la da al comprador (enlace scrollban://license?token=… o código para
 * pegar). La app la comprueba SIN conexión con la clave pública de aquí abajo.
 * Licencia = base64url(JSON {lid, plan, exp, iat}) + "." + base64url(firma DER).
 * exp = 0 → de por vida. Las suscripciones caducan; unos días antes, la app pide la renovada al
 * servidor mandando solo el id de licencia (lid). Si se cancela la suscripción, deja de renovarse.
 */
object Pro {

    private const val PUBLIC_KEY =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEugp0/Q37Ka6ceEZ9Suasj1S+NCMuF2o+f8RP6Bx/wMxP8zCxtIbWbpO+/mFaOa5eFUuJjzl9PODSCwkw9ut3gg=="
    private const val ENDPOINT = "https://www.kimycars.com/api/scrollban-license"
    private const val PREFS = "screenpact_pro"
    private const val KEY_TOKEN = "token"
    private const val KEY_DEV = "dev_pro"
    private const val WORK = "screenpact-license"

    /** Margen tras la fecha de fin: si un día no hay conexión para renovar, no se pierde Pro. */
    private const val GRACE_MS = 3L * 24 * 60 * 60_000L
    private const val RENEW_BEFORE_MS = 5L * 24 * 60 * 60_000L

    data class License(val lid: String, val plan: String, val exp: Long)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Comprueba firma y formato. Devuelve null si la licencia no es nuestra o está mal copiada. */
    fun parse(token: String): License? = runCatching {
        val parts = token.trim().split(".")
        require(parts.size == 2)
        val payload = Base64.decode(parts[0], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val sig = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.decode(PUBLIC_KEY, Base64.DEFAULT)))
        val ok = Signature.getInstance("SHA256withECDSA").run {
            initVerify(key); update(payload); verify(sig)
        }
        require(ok)
        val json = JSONObject(String(payload, Charsets.UTF_8))
        License(json.getString("lid"), json.getString("plan"), json.optLong("exp", 0))
    }.getOrNull()

    fun license(context: Context): License? = prefs(context).getString(KEY_TOKEN, null)?.let(::parse)

    fun isPro(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        if (BuildConfig.DEBUG && prefs(context).getBoolean(KEY_DEV, false)) return true
        val l = license(context) ?: return false
        return l.exp == 0L || now < l.exp + GRACE_MS
    }

    /** Guarda una licencia válida. Devuelve false si no lo es. */
    fun activate(context: Context, token: String): Boolean {
        val l = parse(token) ?: return false
        prefs(context).edit().putString(KEY_TOKEN, token.trim()).apply()
        if (l.exp != 0L) scheduleRenewal(context)
        return true
    }

    /** Solo en la versión de pruebas: activa Pro sin pagar para poder probar sus funciones. */
    fun setDevPro(context: Context, on: Boolean) {
        if (BuildConfig.DEBUG) prefs(context).edit().putBoolean(KEY_DEV, on).apply()
    }

    fun isDevPro(context: Context): Boolean = BuildConfig.DEBUG && prefs(context).getBoolean(KEY_DEV, false)

    private fun scheduleRenewal(context: Context) {
        val request = PeriodicWorkRequestBuilder<LicenseWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Pide la licencia renovada si la actual está a punto de caducar. */
    internal suspend fun renewIfNeeded(context: Context): Boolean = withContext(Dispatchers.IO) {
        val l = license(context) ?: return@withContext true
        if (l.exp == 0L || System.currentTimeMillis() < l.exp - RENEW_BEFORE_MS) return@withContext true
        runCatching {
            val url = URL(ENDPOINT + "?lid=" + URLEncoder.encode(l.lid, "UTF-8"))
            val conn = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 15_000; readTimeout = 15_000 }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val token = JSONObject(body).optString("token", "")
            // Solo se sustituye si la nueva es válida y dura más: nunca se empeora lo que ya hay.
            val fresh = parse(token)
            if (fresh != null && fresh.lid == l.lid && (fresh.exp == 0L || fresh.exp > l.exp)) {
                prefs(context).edit().putString(KEY_TOKEN, token).apply()
            }
            true
        }.getOrDefault(false)
    }
}

/** Estado de Pro para la interfaz; se refresca solo por si llega una licencia mientras la pantalla está abierta. */
@Composable
fun rememberIsPro(): Boolean {
    val context = LocalContext.current
    var pro by remember { mutableStateOf(Pro.isPro(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            pro = Pro.isPro(context)
        }
    }
    return pro
}

class LicenseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = if (Pro.renewIfNeeded(applicationContext)) Result.success() else Result.retry()
}
