package com.screenpact.app.ui.friends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenpact.app.data.crypto.TOTPManager
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.crypto.GroupKeys
import com.screenpact.app.ui.components.BackButton
import com.screenpact.app.ui.components.OutlineButton
import com.screenpact.app.ui.theme.Mono
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.components.UsageRing
import kotlinx.coroutines.delay
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.ui.theme.Strokes
import com.screenpact.app.ui.theme.TextStyles

/** Para quién es la llave: un amigo del pacto o alguien de un grupo (por su id de instalación). */
sealed class KeyFor {
    data class Friend(val friendId: Long) : KeyFor()
    data class GroupMember(val groupId: String, val installId: String, val name: String) : KeyFor()
}

@Composable
fun GenerateCodeScreen(target: KeyFor, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val clipboard = LocalClipboardManager.current
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf<ByteArray?>(null) }
    var iosFriend by remember { mutableStateOf<com.screenpact.app.data.db.Friend?>(null) }
    var code by remember { mutableStateOf("------") }
    var seconds by remember { mutableIntStateOf(30) }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(target) {
        when (target) {
            is KeyFor.Friend -> db.friendDao().getAll().firstOrNull { it.id == target.friendId }?.let {
                // Con iPhone no hay llave de 6 cifras: se le da el código de Tiempo de uso.
                if (it.ios) iosFriend = it else { name = it.name; key = it.generateKey }
            }
            is KeyFor.GroupMember -> db.pactGroupDao().getAll().firstOrNull { it.id == target.groupId }?.let {
                name = target.name
                key = GroupKeys.unlockKey(it.secret, target.installId)
            }
        }
    }

    LaunchedEffect(key) {
        val k = key ?: return@LaunchedEffect
        while (true) {
            val next = TOTPManager.currentCode(k)
            if (next != code) copied = false
            code = next
            seconds = TOTPManager.secondsRemaining()
            delay(1000)
        }
    }

    iosFriend?.let { IosCodeScreen(it, onBack); return }

    Column(
        Modifier.fillMaxSize().padding(horizontal = Space.xl).padding(top = Space.xs, bottom = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) { BackButton(onBack) }
        Spacer(Modifier.height(Space.xxl))
        Text("Llave para", style = MaterialTheme.typography.bodyMedium, color = Sp.TextoSuave)
        Text(name, style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(Space.xxxl))
        UsageRing(seconds / 30f, Sp.Llave, size = Sizes.keyRing, track = Sp.Superficie) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    code.chunked(3).joinToString(" "),
                    style = TextStyles.keyLarge,
                    color = Sp.Llave,
                    modifier = Modifier.semantics { contentDescription = "Llave: ${code.toCharArray().joinToString(" ")}" }
                )
                Spacer(Modifier.height(Space.xs))
                Text("Cambia en $seconds s", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
            }
        }

        Spacer(Modifier.height(Space.xxxl))
        Text(
            "Esta llave solo abre el bloqueo de ${name.ifBlank { "tu amigo" }}. Dásela si de verdad crees que le toca.",
            style = MaterialTheme.typography.bodyMedium,
            color = Sp.TextoSuave,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        OutlineButton(if (copied) "Copiada" else "Copiar llave", {
            clipboard.setText(AnnotatedString(code))
            copied = true
        })
    }
}
