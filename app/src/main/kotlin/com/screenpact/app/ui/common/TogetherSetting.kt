package com.screenpact.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.screenpact.app.data.cloud.Together
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.SpSwitch
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import kotlinx.coroutines.launch

/**
 * Interruptor del ranking con amigos en Ajustes. Encenderlo sube tus minutos; apagarlo borra del
 * servidor todo lo tuyo. Las dos cosas necesitan conexión: si falla, el interruptor no cambia.
 */
@Composable
fun TogetherSettingCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(Together.isEnabled(context)) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    SpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ranking con tus amigos", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xxs))
                Text(
                    if (enabled) "Tus minutos en las apps con límite los ve solo la gente con la que tienes pacto. Al apagarlo se borran del servidor."
                    else "Apagado. Tus minutos no salen del móvil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sp.TextoSuave
                )
                if (failed) {
                    Text("No se ha podido conectar. Vuelve a probar.", style = MaterialTheme.typography.bodySmall, color = Sp.Llave)
                }
            }
            Spacer(Modifier.width(Space.s))
            SpSwitch(checked = enabled, enabled = !busy, onCheckedChange = { on ->
                busy = true
                scope.launch {
                    val ok = runCatching { if (on) Together.enable(context) else Together.disable(context) }.isSuccess
                    busy = false
                    failed = !ok
                    enabled = Together.isEnabled(context)
                }
            })
        }
    }
}
