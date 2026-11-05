package com.screenpact.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.screenpact.app.data.History
import com.screenpact.app.data.History.DayState
import com.screenpact.app.ui.components.ChipButton
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.UsageBar
import com.screenpact.app.ui.theme.Shapes
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.theme.Strokes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
 * Piezas de las rachas: la tarjeta de la racha (Hoy e Historial), la tira de la semana, cada
 * día, las insignias de las metas y el icono de una app. El candado es el sello del pacto:
 * candado verde = día limpio, candado con borde = llegaste al límite pero aguantaste,
 * llave amarilla = ese día pediste una llave.
 */

/** Icono de una app instalada. Mientras carga (o si no existe) deja el hueco vacío. */
@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier, size: Dp = Sizes.appIcon) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(128, 128).asImageBitmap() }.getOrNull()
        }
    }
    Box(modifier.size(size)) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.size(size)) }
    }
}

private val weekLetter = mapOf(
    Calendar.MONDAY to "L", Calendar.TUESDAY to "M", Calendar.WEDNESDAY to "X", Calendar.THURSDAY to "J",
    Calendar.FRIDAY to "V", Calendar.SATURDAY to "S", Calendar.SUNDAY to "D",
)

/** Letra del día de la semana (L, M, X…) de un día "yyyy-MM-dd". */
fun dayLetter(dayKey: String): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dayKey) ?: return ""
    return weekLetter[Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_WEEK)] ?: ""
}

private fun describe(state: DayState): String = when (state) {
    DayState.CLEAN -> "sin llegar al límite"
    DayState.LIMIT -> "llegaste al límite y aguantaste"
    DayState.KEY -> "pediste una llave"
    DayState.EMPTY -> "sin datos"
}

/** Un día como círculo: sello verde, borde verde, llave amarilla o vacío. */
@Composable
fun DayToken(day: History.Day, modifier: Modifier = Modifier) {
    val today = day.key == History.dayKey()
    val look = when (day.state) {
        DayState.CLEAN -> TokenLook(fill = Sp.Pacto, border = null, icon = Icons.Default.Lock, tint = Sp.Tinta)
        DayState.LIMIT -> TokenLook(fill = null, border = Sp.Pacto, icon = Icons.Default.Lock, tint = Sp.Pacto)
        DayState.KEY -> TokenLook(fill = Sp.Llave, border = null, icon = Icons.Default.Key, tint = Sp.Tinta)
        DayState.EMPTY -> TokenLook(fill = null, border = if (today) Sp.TextoSuave else Sp.Borde, icon = null, tint = Sp.TextoSuave)
    }
    DayCircle(day.key, look, describe(day.state), modifier)
}

/**
 * Círculo de un día con su letra encima. Pieza común de la tira de la racha y de los retos:
 * [look] dice el relleno, el borde y el icono; [description] es lo que lee el lector de pantalla.
 */
@Composable
fun DayCircle(dayKey: String, look: TokenLook, description: String, modifier: Modifier = Modifier) {
    val today = dayKey == History.dayKey()
    Column(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = (if (today) "Hoy" else dayLetter(dayKey)) + ": " + description
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            dayLetter(dayKey),
            style = MaterialTheme.typography.labelSmall,
            color = if (today) Sp.Niebla else Sp.TextoSuave,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Spacer(Modifier.height(Space.xxs))
        val (fill, border, icon, tint) = look
        Box(
            Modifier.size(Sizes.dayToken).clip(Shapes.pill)
                .then(if (fill != null) Modifier.background(fill) else Modifier)
                .then(if (border != null) Modifier.border(BorderStroke(Strokes.bold, border), Shapes.pill) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            icon?.let { Icon(it, null, tint = tint, modifier = Modifier.size(Sizes.iconM)) }
        }
    }
}

data class TokenLook(val fill: Color?, val border: Color?, val icon: ImageVector?, val tint: Color)

/** Los últimos 7 días en fila, de lunes… al día de hoy a la derecha. */
@Composable
fun WeekStrip(days: List<History.Day>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.takeLast(7).forEach { DayToken(it) }
    }
}

/**
 * Tarjeta de la racha: el número grande de días sin pedir llave, la otra racha (sin llegar al
 * límite) y el récord, la semana y la siguiente meta. [action] es el botón de la esquina
 * ("Historial" con Pro, "Ver Pro" sin él).
 */
@Composable
fun StreakCard(
    streaks: History.Streaks,
    week: List<History.Day>,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    SpCard(modifier.clickable(enabled = actionLabel != null, onClick = onAction)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Tu racha", style = MaterialTheme.typography.labelMedium, color = Sp.TextoSuave)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${streaks.noKey}",
                        style = MaterialTheme.typography.displayLarge,
                        color = if (streaks.noKey > 0) Sp.Pacto else Sp.Niebla
                    )
                    Spacer(Modifier.width(Space.xs))
                    Text(
                        if (streaks.noKey == 1) "día sin pedir llave" else "días sin pedir llave",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = Space.xs)
                    )
                }
            }
            actionLabel?.let { ChipButton(it, onAction) }
        }
        Text(
            buildString {
                append(if (streaks.noLimit == 1) "1 día sin llegar al límite" else "${streaks.noLimit} días sin llegar al límite")
                if (streaks.bestNoKey > streaks.noKey) append(" · Récord: ${streaks.bestNoKey}")
            },
            style = MaterialTheme.typography.bodySmall,
            color = Sp.TextoSuave
        )
        Spacer(Modifier.height(Space.l))
        WeekStrip(week)
        streaks.nextGoal?.let { goal ->
            Spacer(Modifier.height(Space.l))
            val left = goal - streaks.noKey
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockOpen, null, tint = Sp.TextoSuave, modifier = Modifier.size(Sizes.iconS))
                Spacer(Modifier.width(Space.xs))
                Text(
                    "Insignia de $goal días: " + if (left == 1) "te falta 1" else "te faltan $left",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sp.TextoSuave
                )
            }
            Spacer(Modifier.height(Space.xs))
            UsageBar(streaks.noKey.toFloat() / goal, Sp.Pacto, track = Sp.Linea)
        }
    }
}

/** Insignia de una meta: candado verde si ya la lograste (con tu mejor racha), apagado si no. */
@Composable
fun Medal(goal: Int, earned: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = "Insignia de $goal días: " + if (earned) "conseguida" else "aún no"
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(Sizes.medal)
                .clip(Shapes.pill)
                .then(if (earned) Modifier.background(Sp.Pacto) else Modifier.border(BorderStroke(Strokes.bold, Sp.Borde), Shapes.pill)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$goal",
                style = MaterialTheme.typography.titleLarge,
                color = if (earned) Sp.Tinta else Sp.TextoSuave,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
        Spacer(Modifier.height(Space.xxs))
        Text(
            "días",
            style = MaterialTheme.typography.labelSmall,
            color = if (earned) Sp.Niebla else Sp.TextoSuave,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}
