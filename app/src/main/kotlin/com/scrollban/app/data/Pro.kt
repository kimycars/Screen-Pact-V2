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
 *
 * Compra sin depender de la redirección: al pulsar comprar, la app genera una referencia
 * aleatoria (checkoutUrl) que viaja a Stripe como client_reference_id. El servidor guarda las
 * licencias de ese pago bajo esa referencia y la app las recoge sola al volver (claimPending),
 * aunque la página de gracias no llegue a abrirse por falta de cobertura. La referencia solo la
 * conocen este móvil y Stripe. Del Pack Pacto, la segunda licencia queda guardada para dársela
 * al amigo (gifts).
 */
object Pro {

    private const val PUBLIC_KEY =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEugp0/Q37Ka6ceEZ9Suasj1S+NCMuF2o+f8RP6Bx/wMxP8zCxtIbWbpO+/mFaOa5eFUuJjzl9PODSCwkw9ut3gg=="
    private const val ENDPOINT = "https://www.kimycars.com/api/scrollban-license"
    private const val PREFS = "screenpact_pro"
    private const val KEY_TOKEN = "token"
    private const val KEY_DEV = "dev_pro"
    private const val WORK = "screenpact-license"
    private const val KEY_PENDING = "pending_refs"
    private const val KEY_GIFTS = "gifts"
    private const val PENDING_MS = 30L * 24 * 60 * 60_000L

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

    /** Enlace de pago con una referencia nueva para recoger la licencia sin la página de gracias. */
    fun checkoutUrl(context: Context, link: String): String {
        val bytes = ByteArray(18).also { java.security.SecureRandom().nextBytes(it) }
        val ref = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val now = System.currentTimeMillis()
        savePending(context, pending(context, now) + (ref to now))
        return link + (if ('?' in link) "&" else "?") + "client_reference_id=" + ref
    }

    fun hasPending(context: Context): Boolean = pending(context, System.currentTimeMillis()).isNotEmpty()

    /** Licencias del Pack Pacto que aún hay que dar al amigo. */
    fun gifts(context: Context): List<String> =
        prefs(context).getString(KEY_GIFTS, "").orEmpty().split(" ").filter { parse(it) != null }

    fun removeGift(context: Context, token: String) =
        prefs(context).edit().putString(KEY_GIFTS, gifts(context).filter { it != token }.joinToString(" ")).apply()

    private fun pending(context: Context, now: Long): List<Pair<String, Long>> =
        prefs(context).getString(KEY_PENDING, "").orEmpty().split(",").mapNotNull { e ->
            val (ref, at) = e.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            at.toLongOrNull()?.takeIf { now - it < PENDING_MS }?.let { ref to it }
        }

    private fun savePending(context: Context, list: List<Pair<String, Long>>) =
        prefs(context).edit().putString(KEY_PENDING, list.joinToString(",") { "${it.first}:${it.second}" }).apply()

    /**
     * Pregunta al servidor por las compras hechas desde este móvil que aún no han llegado.
     * Devuelve true si ha activado alguna licencia.
     */
    suspend fun claimPending(context: Context): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        var activated = false
        val left = pending(context, now).filter { (ref, _) ->
            val tokens = runCatching {
                val url = URL(ENDPOINT + "?ref=" + URLEncoder.encode(ref, "UTF-8"))
                val conn = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 15_000; readTimeout = 15_000 }
                val ok = conn.responseCode == 200
                val body = if (ok) conn.inputStream.bufferedReader().use { it.readText() } else ""
                conn.disconnect()
                if (!ok) return@runCatching null
                val arr = JSONObject(body).getJSONArray("licenses")
                (0 until arr.length()).map { arr.getJSONObject(it).getString("token") }.filter { parse(it) != null }
            }.getOrNull()
            if (tokens.isNullOrEmpty()) return@filter true // aún no ha llegado: se vuelve a preguntar
            val extra = if (!isPro(context, now) && activate(context, tokens.first())) {
                activated = true
                tokens.drop(1)
            } else tokens
            if (extra.isNotEmpty()) {
                prefs(context).edit().putString(KEY_GIFTS, (gifts(context) + extra).distinct().joinToString(" ")).apply()
            }
            false
        }
        savePending(context, left)
        activated
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
