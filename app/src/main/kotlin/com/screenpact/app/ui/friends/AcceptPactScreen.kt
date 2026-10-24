package com.screenpact.app.ui.friends

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.screenpact.app.data.Pairing
import com.screenpact.app.data.crypto.PairingPayload
import com.screenpact.app.ui.common.Invite
import com.screenpact.app.ui.components.Avatar
import com.screenpact.app.ui.components.BackButton
import com.screenpact.app.ui.components.PrimaryButton
import com.screenpact.app.ui.components.QuietButton
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.StepLine
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Invitación que ha llegado por enlace y espera a que la app la abra (MainActivity → MainNav). */
object IncomingPact {
    val text = MutableStateFlow<String?>(null)

    fun from(intent: Intent?) {
        val uri = intent?.data ?: return
        val isPact = (uri.scheme == "screenpact" && uri.host == "pair") ||
            (uri.scheme == "https" && uri.path.orEmpty().startsWith("/screenpact/pacto"))
        if (isPact) text.value = uri.toString()
    }
}

/**
 * Abrir una invitación (o respuesta) a distancia. Con una invitación: se acepta y la app da
 * la respuesta para mandar. Con una respuesta: el pacto queda sellado en este móvil.
 */
@Composable
fun AcceptPactScreen(text: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val payload = remember(text) { PairingPayload.decode(text) }
    var result by remember { mutableStateOf<Pairing.Result?>(null) }
    var working by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screen)
            .padding(top = Space.xs, bottom = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.m)
    ) {
        BackButton(onDone)
        val r = result
        when {
            payload == null -> {
                Text("Esta invitación no vale", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (PairingPayload.isLegacy(text)) "Es de una versión antigua de ScreenPact. Pide que actualicen la app y te la vuelvan a mandar."
                    else "El enlace está incompleto o no es de ScreenPact. Pide que te lo vuelvan a mandar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sp.TextoSuave
                )
                PrimaryButton("Volver", onDone)
            }

            r is Pairing.Result.Failed -> {
                Text("No se ha podido sellar", style = MaterialTheme.typography.headlineLarge)
                Text(r.message, style = MaterialTheme.typography.bodyMedium, color = Sp.TextoSuave)
                PrimaryButton("Volver", onDone)
            }

            r is Pairing.Result.Paired && r.reply != null -> {
                Text("Último paso", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Ya tienes la llave de ${r.name}. Mándale tu respuesta para que el pacto vaya en los dos sentidos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sp.TextoSuave
                )
                PrimaryButton("Mandar respuesta a ${r.name}", { Invite.sharePact(context, r.reply) })
                QuietButton("Ya se la he mandado", onDone)
            }

            r is Pairing.Result.Paired -> {
                Text("Pacto sellado", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${r.name} y tú ya tenéis la llave del otro. Su llave empezará a valer dentro de 24 h.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sp.TextoSuave
                )
                PrimaryButton("Listo", onDone)
            }

            else -> {
                Avatar(payload.name, size = Sizes.medal)
                Text(
                    if (payload.replyTo == null) "${payload.name} quiere sellar un pacto contigo"
                    else "${payload.name} ha aceptado tu pacto",
                    style = MaterialTheme.typography.headlineLarge
                )
                SpCard {
                    StepLine("Guardas su llave", icon = Icons.Default.Key, detail = "Si se pasa del límite, tú decides si le das más tiempo.", badge = Sp.Linea)
                    StepLine("Y tú le das la tuya", icon = Icons.Default.Send,
                        detail = if (payload.replyTo == null) "Al aceptar, tu app te da una respuesta para mandarle." else "Con esto el pacto queda cerrado.",
                        badge = Sp.Linea)
                    StepLine("Empieza en 24 h", icon = Icons.Default.Schedule, detail = "Los pactos a distancia tardan un día en poder dar llaves.", badge = Sp.Linea)
                }
                Spacer(Modifier.height(Space.xs))
                PrimaryButton(if (payload.replyTo == null) "Sellar el pacto" else "Cerrar el pacto", {
                    working = true
                    scope.launch { result = Pairing.accept(context, payload, remote = true); working = false }
                }, enabled = !working)
                QuietButton("Ahora no", onDone)
            }
        }
    }
}
