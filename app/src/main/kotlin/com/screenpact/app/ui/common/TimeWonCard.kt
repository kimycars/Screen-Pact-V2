package com.screenpact.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.screenpact.app.data.TimeWon
import com.screenpact.app.ui.components.ChipButton
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.UsageBar
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import kotlin.math.abs

private fun minutesOf(ms: Long) = (ms / 60_000L).toInt()

/**
 * Tiempo ganado en los últimos días terminados: la cifra grande, tu media de antes frente a la
 * de ahora y el total desde que empezaste. Si vas peor que antes, lo dice sin sermón.
 */
@Composable
fun TimeWonCard(summary: TimeWon.Summary, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SpCard(modifier) {
        if (summary.days == 0) {
            Text("Tiempo recuperado", style = MaterialTheme.typography.labelMedium, color = Sp.TextoSuave)
            Spacer(Modifier.height(Space.xxs))
            Text("Mañana verás tu primer día", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Space.xxs))
            Text(
                "Cada día se compara con lo que usabas cada app antes de ponerle límite.",
                style = MaterialTheme.typography.bodySmall,
                color = Sp.TextoSuave
            )
            return@SpCard
        }

        val won = minutesOf(summary.wonMs)
        val period = if (summary.days == 1) "ayer" else "en los últimos ${summary.days} días"
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (won > 0) "Has recuperado" else "Vas por encima de antes",
                    style = MaterialTheme.typography.labelMedium,
                    color = Sp.TextoSuave
                )
                Text(
                    formatMinutes(abs(won)),
                    style = MaterialTheme.typography.displayLarge,
                    color = if (won > 0) Sp.Pacto else Sp.Llave
                )
                Text(
                    if (won > 0) period else "$period, más que antes del límite",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sp.TextoSuave
                )
            }
            if (won > 0) {
                ChipButton("Compartir", {
                    Invite.shareAchievement(context, "He recuperado ${formatMinutes(won)} de móvil $period.")
                })
            }
        }

        Spacer(Modifier.height(Space.l))
        val before = summary.beforePerDayMs
        val now = summary.nowPerDayMs
        val max = maxOf(before, now, 1L).toFloat()
        CompareRow("Antes", minutesOf(before), before / max, Sp.TextoSuave)
        Spacer(Modifier.height(Space.s))
        CompareRow("Ahora", minutesOf(now), now / max, if (now <= before) Sp.Pacto else Sp.Llave)

        val total = minutesOf(summary.totalWonMs)
        if (total > won && total > 0) {
            Spacer(Modifier.height(Space.m))
            Text(
                "Desde que empezaste: ${formatMinutes(total)}",
                style = MaterialTheme.typography.bodySmall,
                color = Sp.TextoSuave
            )
        }
    }
}

/** Una barra de la comparación: "Antes 2 h 10 al día". */
@Composable
private fun CompareRow(label: String, minutesPerDay: Int, fraction: Float, color: androidx.compose.ui.graphics.Color) {
    Column(Modifier.semantics(mergeDescendants = true) { contentDescription = "$label: ${formatMinutes(minutesPerDay)} al día" }) {
        Row(Modifier.clearAndSetSemantics { }) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${formatMinutes(minutesPerDay)} al día", style = MaterialTheme.typography.bodyMedium, color = Sp.TextoSuave)
        }
        Spacer(Modifier.height(Space.xxs))
        UsageBar(fraction, color, track = Sp.Linea, modifier = Modifier.clearAndSetSemantics { })
    }
}
