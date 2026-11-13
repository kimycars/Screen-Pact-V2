package com.scrollban.app.ui.friends

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
import com.scrollban.app.ui.common.Invite
import com.scrollban.app.ui.common.formatMinutes
import com.scrollban.app.ui.components.Avatar
import com.scrollban.app.ui.components.ChipButton
import com.scrollban.app.ui.components.PrimaryButton
import com.scrollban.app.ui.components.SbCard
import com.scrollban.app.ui.components.StepLine
import com.scrollban.app.ui.theme.Sizes
import com.scrollban.app.ui.theme.Sb
import com.scrollban.app.ui.theme.Space

/**
 * Juntos apagado: qué se gana al encenderlo y qué se comparte, dicho claro antes de pulsar.
 * [error]: el último intento falló (sin conexión).
 */
@Composable
fun TogetherOffCard(error: Boolean, onEnable: () -> Unit) {
    SbCard {
        StepLine(
            "Ranking con tus amigos",
            icon = Icons.Default.Groups,
            detail = "Quién usa menos el móvil, cuánto habéis recuperado juntos y vuestros retos.",
            badge = Sb.Linea
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            "Para verlo, tus minutos en las apps con límite se guardan en el servidor de ScrollBan y solo los ve la gente con la que tienes pacto. Sin correo ni cuenta. Si lo apagas en Ajustes, se borra todo.",
            style = MaterialTheme.typography.bodySmall,
            color = Sb.TextoSuave
        )
        if (error) {
            Spacer(Modifier.height(Space.xs))
            Text("No se ha podido conectar. Mira tu conexión y vuelve a probar.", style = MaterialTheme.typography.bodySmall, color = Sb.Llave)
        }
        Spacer(Modifier.height(Space.m))
        PrimaryButton("Activar el ranking", onEnable)
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
    SbCard {
        if (groupWon > 0) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Juntos habéis recuperado", style = MaterialTheme.typography.labelMedium, color = Sb.TextoSuave)
                    Text(formatMinutes(groupWon), style = MaterialTheme.typography.displayLarge, color = Sb.Pacto)
                    Text("en los últimos 30 días", style = MaterialTheme.typography.bodySmall, color = Sb.TextoSuave)
                }
                ChipButton("Compartir", {
                    Invite.shareAchievement(context, "Mis amigos y yo hemos recuperado ${formatMinutes(groupWon)} de móvil este mes.")
                })
            }
            Spacer(Modifier.height(Space.l))
        }
        Text("Esta semana", style = MaterialTheme.typography.titleMedium)
        Text("Uso de las apps con límite, últimos 7 días", style = MaterialTheme.typography.bodySmall, color = Sb.TextoSuave)
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
            color = if (position == 1) Sb.Pacto else Sb.TextoSuave,
            modifier = Modifier.widthIn(min = Space.xl)
        )
        Avatar(name, size = Sizes.avatar, color = if (r.isMe) Sb.Pacto else Sb.Llave)
        Spacer(Modifier.width(Space.s))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Sb.TextoSuave) }
        }
        Spacer(Modifier.width(Space.s))
        used?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
    }
    Spacer(Modifier.height(Space.s))
}
