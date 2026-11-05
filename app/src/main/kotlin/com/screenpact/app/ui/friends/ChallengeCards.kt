package com.screenpact.app.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.screenpact.app.data.Challenges
import com.screenpact.app.data.History
import com.screenpact.app.data.Challenges.DayResult
import com.screenpact.app.data.db.AppLimit
import com.screenpact.app.data.db.Challenge
import com.screenpact.app.ui.common.DayCircle
import com.screenpact.app.ui.common.Invite
import com.screenpact.app.ui.common.TokenLook
import com.screenpact.app.ui.components.ChipButton
import com.screenpact.app.ui.components.ChoiceChip
import com.screenpact.app.ui.components.OutlineButton
import com.screenpact.app.ui.components.PrimaryButton
import com.screenpact.app.ui.components.QuietButton
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.StepLine
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space

/** Sin reto: qué es y el botón para empezar uno. */
@Composable
fun NoChallengeCard(onStart: () -> Unit) {
    SpCard {
        StepLine(
            "Ponte un reto con fecha",
            icon = Icons.Default.Flag,
            detail = "Una semana sin TikTok, un finde con Instagram a 15 minutos… Cada día cumplido suma.",
            badge = Sp.Linea
        )
        Spacer(Modifier.height(Space.s))
        PrimaryButton("Empezar un reto", onStart)
    }
}

/** El reto en curso o recién terminado: los días como círculos y qué hacer ahora. */
@Composable
fun ChallengeCard(progress: Challenges.Progress, onNew: () -> Unit, onQuit: () -> Unit, partners: List<String> = emptyList()) {
    val context = LocalContext.current
    val c = progress.challenge
    val share = { Invite.shareAchievement(context, Challenges.shareText(progress)) }
    SpCard {
        Text(
            when {
                progress.won -> "Reto superado"
                progress.finished -> "Reto terminado"
                else -> "Tu reto"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (progress.won) Sp.Pacto else Sp.TextoSuave
        )
        Spacer(Modifier.height(Space.xxs))
        Text(Challenges.title(c), style = MaterialTheme.typography.titleLarge)
        if (partners.isNotEmpty()) {
            Text("Con ${partners.joinToString(" y ")}", style = MaterialTheme.typography.bodyMedium, color = Sp.Pacto)
        }
        Spacer(Modifier.height(Space.xxs))
        val today = progress.days.firstOrNull { it.first == History.dayKey() }?.second
        Text(
            when {
                progress.finished -> "${progress.done} de ${progress.days.size} días cumplidos."
                progress.todayIndex == null -> "Empieza mañana. Hoy ya habías pasado del tope."
                today == DayResult.TODAY_OVER -> "Día ${progress.todayIndex} de ${progress.days.size}. Hoy te has pasado."
                else -> "Día ${progress.todayIndex} de ${progress.days.size}. Hoy vas bien."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (today == DayResult.TODAY_OVER) Sp.Llave else Sp.TextoSuave
        )
        Spacer(Modifier.height(Space.l))
        ChallengeDays(progress)
        Spacer(Modifier.height(Space.xs))
        if (progress.finished) {
            PrimaryButton("Otro reto", onNew)
            Spacer(Modifier.height(Space.xs))
            OutlineButton("Compartir", share)
        } else {
            Row {
                ChipButton("Compartir", share)
                Spacer(Modifier.weight(1f))
                QuietButton("Dejar el reto", onQuit)
            }
        }
    }
}

/** Los días del reto como círculos, en filas de una semana. */
@Composable
private fun ChallengeDays(progress: Challenges.Progress) {
    progress.days.chunked(7).forEach { week ->
        // Una semana entera ocupa todo el ancho, como la tira de la racha; un reto corto va a la izquierda.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (week.size == 7) Arrangement.SpaceBetween else Arrangement.spacedBy(Space.s)) {
            week.forEach { (day, result) -> DayCircle(day, lookOf(result), describe(result)) }
        }
        Spacer(Modifier.height(Space.s))
    }
}

/**
 * El reto de un amigo, con sus días. [canJoin]: tienes límite en esa app (si no, no se puede
 * medir y se explica). Unirse lo hace también tuyo, con las mismas fechas.
 */
@Composable
fun FriendChallengeCard(name: String, progress: Challenges.Progress, canJoin: Boolean, onJoin: () -> Unit) {
    val c = progress.challenge
    SpCard {
        Text(
            when {
                progress.won -> "$name ha superado su reto"
                progress.finished -> "Reto de $name, terminado"
                else -> "Reto de $name"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (progress.won) Sp.Pacto else Sp.TextoSuave
        )
        Spacer(Modifier.height(Space.xxs))
        Text(Challenges.title(c), style = MaterialTheme.typography.titleMedium)
        Text(
            when {
                progress.finished -> "${progress.done} de ${progress.days.size} días cumplidos."
                progress.todayIndex == null -> "Empieza mañana."
                else -> "Día ${progress.todayIndex} de ${progress.days.size}. Lleva ${progress.done} cumplidos."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Sp.TextoSuave
        )
        Spacer(Modifier.height(Space.m))
        ChallengeDays(progress)
        if (!progress.finished) {
            if (canJoin) {
                // A mitad de reto, para quien se une cuenta desde hoy y acaba el mismo día.
                val left = Challenges.daysLeftFrom(c, History.dayKey())
                if (left < c.days) {
                    Text(
                        if (left == 1) "Si te unes, cuenta desde hoy: queda 1 día." else "Si te unes, cuenta desde hoy: quedan $left días.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sp.TextoSuave
                    )
                    Spacer(Modifier.height(Space.xs))
                }
                ChipButton("Unirme al reto", onJoin)
            } else {
                Text(
                    "Para unirte, ponle un límite a ${c.appName}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sp.TextoSuave
                )
            }
        }
    }
}

private fun lookOf(r: DayResult): TokenLook = when (r) {
    DayResult.DONE -> TokenLook(fill = Sp.Pacto, border = null, icon = Icons.Default.Check, tint = Sp.Tinta)
    DayResult.FAILED -> TokenLook(fill = null, border = Sp.Borde, icon = Icons.Default.Close, tint = Sp.TextoSuave)
    DayResult.TODAY_OK -> TokenLook(fill = null, border = Sp.Pacto, icon = null, tint = Sp.Pacto)
    DayResult.TODAY_OVER -> TokenLook(fill = null, border = Sp.Llave, icon = Icons.Default.Close, tint = Sp.Llave)
    DayResult.UPCOMING -> TokenLook(fill = null, border = Sp.Linea, icon = null, tint = Sp.TextoSuave)
}

private fun describe(r: DayResult): String = when (r) {
    DayResult.DONE -> "cumplido"
    DayResult.FAILED -> "no cumplido"
    DayResult.TODAY_OK -> "vas bien"
    DayResult.TODAY_OVER -> "te has pasado"
    DayResult.UPCOMING -> "pendiente"
}

/**
 * Crear un reto: qué app (de las que ya tienen límite, porque son las que se miden), cuánto al
 * día y cuántos días. Abajo, cómo se va a llamar el reto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartChallengeDialog(limits: List<AppLimit>, onDismiss: () -> Unit, onStart: (AppLimit, Int, Int) -> Unit) {
    var app by remember { mutableStateOf(limits.first()) }
    var minutes by remember { mutableStateOf(Challenges.MINUTE_OPTIONS.first()) }
    var days by remember { mutableStateOf(7) }
    val preview = Challenge("", app.packageName, app.appName, minutes, "", days)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo reto") },
        text = {
            Column {
                Label("App")
                FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    limits.forEach { l -> ChoiceChip(l.appName, l.packageName == app.packageName, { app = l }) }
                }
                Label("Al día, como mucho")
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Challenges.MINUTE_OPTIONS.forEach { m -> ChoiceChip(if (m == 0) "Nada" else "$m min", m == minutes, { minutes = m }) }
                }
                Label("Durante")
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Challenges.DAY_OPTIONS.forEach { d ->
                        ChoiceChip(
                            when (d) { 7 -> "1 semana"; 14 -> "2 semanas"; else -> "$d días" },
                            d == days,
                            { days = d }
                        )
                    }
                }
                Spacer(Modifier.height(Space.l))
                Text(Challenges.title(preview), style = MaterialTheme.typography.titleMedium, color = Sp.Llave)
            }
        },
        confirmButton = { TextButton(onClick = { onStart(app, minutes, days) }) { Text("Empezar reto") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Sp.TextoSuave) } }
    )
}

@Composable
private fun Label(text: String) {
    Spacer(Modifier.height(Space.m))
    Text(text, style = MaterialTheme.typography.labelMedium, color = Sp.TextoSuave)
    Spacer(Modifier.height(Space.xs))
}

/** Confirmación de dejar el reto: se borra y no cuenta. */
@Composable
fun QuitChallengeDialog(c: Challenge, onDismiss: () -> Unit, onQuit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Dejar el reto?") },
        text = { Text("«${Challenges.title(c)}» se borra con lo que llevas.", color = Sp.TextoSuave) },
        confirmButton = { TextButton(onClick = onQuit) { Text("Dejar el reto", color = Sp.Llave) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Seguir con él") } }
    )
}
