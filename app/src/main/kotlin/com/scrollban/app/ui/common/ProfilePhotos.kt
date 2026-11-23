package com.scrollban.app.ui.common

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.scrollban.app.data.Prefs
import com.scrollban.app.data.cloud.Photos
import com.scrollban.app.data.cloud.Together
import com.scrollban.app.ui.components.Avatar
import com.scrollban.app.ui.components.ChipButton
import com.scrollban.app.ui.components.QuietButton
import com.scrollban.app.ui.components.SbCard
import com.scrollban.app.ui.theme.Sb
import com.scrollban.app.ui.theme.Sizes
import com.scrollban.app.ui.theme.Space
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Tu foto de perfil, o null si no has elegido ninguna. */
@Composable
fun rememberOwnPhoto(): ImageBitmap? {
    val context = LocalContext.current
    return rememberPhotoFile { Photos.ownFile(context) }
}

/** La foto de alguien de Juntos por su id en el servidor, o null. */
@Composable
fun rememberPersonPhoto(uid: String?): ImageBitmap? {
    val context = LocalContext.current
    return rememberPhotoFile(uid) { uid?.let { Photos.personFile(context, it) } }
}

/** La foto de un amigo con pacto, o null (también sin conexión: sale de lo ya bajado). */
@Composable
fun rememberFriendPhoto(friendId: Long): ImageBitmap? {
    val context = LocalContext.current
    return rememberPhotoFile(friendId) { Photos.friendUid(context, friendId)?.let { Photos.personFile(context, it) } }
}

@Composable
private fun rememberPhotoFile(key: Any? = null, file: () -> File?): ImageBitmap? {
    val version by Photos.changes.collectAsState()
    val photo by produceState<ImageBitmap?>(null, key, version) {
        value = withContext(Dispatchers.IO) {
            file()?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
        }
    }
    return photo
}

/**
 * Elegir o quitar tu foto de perfil (Ajustes). Se elige con el selector de fotos de Android, que
 * no pide permiso para ver toda la galería. Solo sale del móvil con el ranking encendido.
 */
@Composable
fun OwnPhotoCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photo = rememberOwnPhoto()
    val name by produceState("") { value = Prefs.getOwnName(context) }
    var failed by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { failed = !Photos.setOwn(context, uri) }
    }
    SbCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name, size = Sizes.medal, color = Sb.Pacto, photo = photo)
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text("Tu foto", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xxs))
                Text(
                    if (Together.isEnabled(context)) "La ven solo tus amigos con pacto y tus grupos. Viaja cifrada."
                    else "Se comparte cuando actives el ranking con tus amigos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sb.TextoSuave
                )
                if (failed) Text("No se ha podido abrir esa imagen. Prueba con otra.", style = MaterialTheme.typography.bodySmall, color = Sb.Llave)
            }
        }
        Spacer(Modifier.height(Space.s))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipButton(if (photo == null) "Elegir foto" else "Cambiar", {
                failed = false
                pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            })
            if (photo != null) {
                Spacer(Modifier.width(Space.s))
                QuietButton("Quitar", { scope.launch { Photos.clearOwn(context) } })
            }
        }
    }
}
