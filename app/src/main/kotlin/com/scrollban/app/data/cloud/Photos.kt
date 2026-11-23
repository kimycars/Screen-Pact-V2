package com.scrollban.app.data.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.scrollban.app.data.crypto.GroupKeys
import com.scrollban.app.data.db.AppDatabase
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Fotos de perfil de Juntos.
 *
 * Cada uno elige la suya en Ajustes. Solo sale del móvil con el ranking encendido, y sube cifrada
 * una vez por grupo (cada pacto es un grupo de 2, más los grupos de 3 o más) con una clave que
 * sale del pacto o del secreto del grupo: el servidor guarda la caja pero no puede abrirla.
 * Las fotos de los demás se bajan solo cuando cambian y se guardan en el móvil, así se ven
 * también sin conexión. Al apagar el ranking se borran las de los demás y la propia del servidor.
 */
object Photos {

    private const val PREFS = "scrollban_photos"
    private const val KEY_OWN_AT = "own_at"
    private const val KEY_UPLOADED = "uploaded"

    /** Lado de la foto en píxeles y calidad del JPEG: unos 10 KB, nítida en el avatar más grande. */
    private const val SIZE_PX = 192
    private const val QUALITY = 80

    private val _changes = MutableStateFlow(0)

    /** Cambia cada vez que cambia alguna foto guardada en el móvil (para redibujar los avatares). */
    val changes: StateFlow<Int> = _changes

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun ownFile(context: Context) = File(context.filesDir, "photo_me.jpg")

    fun personFile(context: Context, uid: String) = File(File(context.filesDir, "photos").apply { mkdirs() }, "$uid.jpg")

    /** Id en el servidor de un amigo con pacto (se apunta al pedir el tablero). */
    fun friendUid(context: Context, friendId: Long): String? = prefs(context).getString("f_$friendId", null)

    /** Guarda como tu foto la imagen [uri] (recortada al centro y reducida) y la sube si toca. */
    suspend fun setOwn(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val jpeg = runCatching { toJpeg(context, uri) }.getOrNull() ?: return@withContext false
        ownFile(context).writeBytes(jpeg)
        ownChanged(context)
        true
    }

    suspend fun clearOwn(context: Context) = withContext(Dispatchers.IO) {
        ownFile(context).delete()
        ownChanged(context)
    }

    private suspend fun ownChanged(context: Context) {
        prefs(context).edit().putLong(KEY_OWN_AT, System.currentTimeMillis()).remove(KEY_UPLOADED).apply()
        _changes.value++
        if (Together.isEnabled(context)) runCatching { Together.sync(context, force = true) }
    }

    /**
     * Sube tu foto (o la quita) en cada grupo de [joined] que aún no tiene la última. Lo llama
     * [Together.sync]. Si nunca has elegido foto, no sube nada.
     */
    internal suspend fun upload(context: Context, joined: Set<String>) {
        val p = prefs(context)
        val ownAt = p.getLong(KEY_OWN_AT, 0L)
        if (ownAt == 0L) return
        val done = p.getStringSet(KEY_UPLOADED, emptySet())!!.toMutableSet()
        val jpeg = ownFile(context).takeIf { it.exists() }?.readBytes()
        val db = AppDatabase.get(context)
        val boxes = db.friendDao().getAll().filter { !it.ios }.map { f ->
            Together.groupOf(f).first to { j: ByteArray -> GroupKeys.sealPhotoForFriend(f, j) }
        } + db.pactGroupDao().getAll().map { g -> g.id to { j: ByteArray -> GroupKeys.sealPhotoForGroup(g.secret, j) } }
        for ((group, seal) in boxes) {
            val mark = "$group@$ownAt"
            if (group !in joined || mark in done) continue
            Cloud.rpc(context, "set_photo", JSONObject().put("p_group", group).put("p_photo", jpeg?.let(seal) ?: JSONObject.NULL))
            done += mark
            p.edit().putStringSet(KEY_UPLOADED, done.toSet()).apply()
        }
    }

    /** Una foto que el servidor dice que existe: de [uid] en [group], cambiada en [version]. */
    internal class Seen(val group: String, val uid: String, val version: Long?)

    /**
     * Baja las fotos que han cambiado desde la última vez y borra las que ya no están.
     * [open] abre la caja de un grupo con su clave (null si el grupo ya no es tuyo).
     */
    internal suspend fun download(context: Context, seen: List<Seen>, friendUids: Map<Long, String>, open: (String, String) -> ByteArray?) {
        val p = prefs(context)
        val edit = p.edit()
        friendUids.forEach { (id, uid) -> edit.putString("f_$id", uid) }
        edit.apply()
        var changed = false
        // De cada persona, la foto más reciente de todos los grupos que compartís.
        for ((uid, entries) in seen.groupBy { it.uid }) {
            val latest = entries.filter { it.version != null }.maxByOrNull { it.version!! }
            val file = personFile(context, uid)
            if (latest == null) {
                if (file.delete()) changed = true
                p.edit().remove("v_$uid").apply()
                continue
            }
            if (p.getLong("v_$uid", 0L) == latest.version && file.exists()) continue
            val box = runCatching {
                Cloud.rpc(context, "member_photo", JSONObject().put("p_group", latest.group).put("p_user", uid))
            }.getOrNull()?.let { runCatching { JSONTokener(it).nextValue() as? String }.getOrNull() } ?: continue
            val jpeg = open(latest.group, box) ?: continue
            if (BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) == null) continue
            withContext(Dispatchers.IO) { file.writeBytes(jpeg) }
            p.edit().putLong("v_$uid", latest.version!!).apply()
            changed = true
        }
        if (changed) _changes.value++
    }

    /** Al apagar el ranking: fuera las fotos de los demás y la marca de lo subido. La tuya se queda en el móvil. */
    fun forgetOthers(context: Context) {
        File(context.filesDir, "photos").deleteRecursively()
        prefs(context).edit().clear().apply()
        if (ownFile(context).exists()) prefs(context).edit().putLong(KEY_OWN_AT, System.currentTimeMillis()).apply()
        _changes.value++
    }

    private fun toJpeg(context: Context, uri: Uri): ByteArray {
        val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
                // Reduce ya al decodificar: una foto de 12 MP no cabe cómoda en memoria.
                val scale = (minOf(info.size.width, info.size.height) / (SIZE_PX * 2)).coerceAtLeast(1)
                d.setTargetSize(info.size.width / scale, info.size.height / scale)
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
            val sample = (minOf(bounds.outWidth, bounds.outHeight) / (SIZE_PX * 2)).coerceAtLeast(1)
            context.contentResolver.openInputStream(uri)!!.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })!!
            }
        }
        val side = minOf(source.width, source.height)
        val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
        val small = Bitmap.createScaledBitmap(square, SIZE_PX, SIZE_PX, true)
        return ByteArrayOutputStream().use { out ->
            small.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            out.toByteArray()
        }
    }
}
