package com.screenpact.app.ui.friends

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.screenpact.app.ui.common.Invite
import com.screenpact.app.ui.common.formatMinutes
import com.screenpact.app.ui.components.Avatar
import com.screenpact.app.ui.components.ChipButton
import com.screenpact.app.ui.components.PrimaryButton
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.StepLine
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space

/**
 * Juntos apagado: qué se gana al encenderlo y qué se comparte, dicho claro antes de pulsar.
 * [error]: el último intento falló (sin conexión).
 */
@Composable
fun TogetherOffCard(busy: Boolean, error: Boolean, onEnable: () -> Unit) {
    SpCard {
        StepLine(
            "Ranking con tus amigos",
            icon = Icons.Default.Groups,
            detail = "Quién usa menos el móvil, cuánto habéis recuperado juntos y vuestros retos.",
            badge = Sp.Linea
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            "Para verlo, tus minutos en las apps con límite se guardan en el servidor de Scrollban y solo los ve la gente con la que tienes pacto. Sin correo ni cuenta. Si lo apagas en Ajustes, se borra todo.",
            style = MaterialTheme.typography.bodySmall,
            color = Sp.TextoSuave
        )
        if (error) {
            Spacer(Modifier.height(Space.xs))
            Text("No se ha podido conectar. Mira tu conexión y vuelve a probar.", style = MaterialTheme.typography.bodySmall, color = Sp.Llave)
        }
        Spacer(Modifier.height(Space.m))
        PrimaryButton(if (busy) "Activando…" else "Activar el ranking", onEnable, enabled = !busy)
    }
}

/** Una fila del ranking: quién, cuánto ha usado en 7 días y cuánto ha recuperado. */
data class RankRow(val name: String, val isMe: Boolean, val usedMs: Long?, val wonMs: Long)

/**
 * El ranking de la semana: ordenado de menos a más uso. Quien aún no ha encendido el ranking va
 * al final, sin cifras. Arriba, lo que habéis recuperado juntos en el último mes.
 */
@Composable
fun RankingCard(rows: List<RankRow>, groupWonMs: Long) {
    val context = LocalContext.current
    val groupWon = (groupWonMs / 60_000L).toInt()
    SpCard {
        if (groupWon > 0) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Juntos habéis recuperado", style = MaterialTheme.typography.labelMedium, color = Sp.TextoSuave)
                    Text(formatMinutes(groupWon), style = MaterialTheme.typography.displayLarge, color = Sp.Pacto)
                    Text("en los últimos 30 días", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
                }
                ChipButton("Compartir", {
                    Invite.shareAchievement(context, "Mis amigos y yo hemos recuperado ${formatMinutes(groupWon)} de móvil este mes.")
                })
            }
            Spacer(Modifier.height(Space.l))
        }
        Text("Esta semana", style = MaterialTheme.typography.titleMedium)
        Text("Uso de las apps con límite, últimos 7 días", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
        Spacer(Modifier.height(Space.s))
        val sorted = rows.sortedWith(compareBy<RankRow> { it.usedMs == null }.thenBy { it.usedMs ?: 0L })
        var position = 0
        sorted.forEach { r ->
            if (r.usedMs != null) position++
            RankLine(if (r.usedMs != null) position else null, r)
        }
    }
}

@Composable
private fun RankLine(position: Int?, r: RankRow) {
    val used = r.usedMs?.let { formatMinutes((it / 60_000L).toInt()) }
    val won = (r.wonMs / 60_000L).toInt()
    val detail = when {
        used == null -> "Aún no ha activado el ranking"
        won > 0 -> "Ha recuperado ${formatMinutes(won)}"
        else -> null
    }
    val name = if (r.isMe) "Tú" else r.name
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = listOfNotNull(position?.let { "Puesto $it" }, name, used, detail).joinToString(". ")
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            position?.toString() ?: "–",
            style = MaterialTheme.typography.titleMedium,
            color = if (position == 1) Sp.Pacto else Sp.TextoSuave,
            modifier = Modifier.widthIn(min = Space.xl)
        )
        Avatar(name, size = Sizes.avatar, color = if (r.isMe) Sp.Pacto else Sp.Llave)
        Spacer(Modifier.width(Space.s))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave) }
        }
        Spacer(Modifier.width(Space.s))
        used?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
    }
    Spacer(Modifier.height(Space.s))
}
