package com.screenpact.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.screenpact.app.data.History
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.DayRecord
import com.screenpact.app.ui.components.BackButton
import com.screenpact.app.ui.components.ScreenTitle
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.UsageBar
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.ui.common.Medal
import com.screenpact.app.ui.common.StreakCard
import com.screenpact.app.ui.common.WeekStrip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Locale

/** Historial (Pro): racha, calendario de 4 semanas, récords, insignias y el detalle de la semana. */
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val limits by db.appLimitDao().observeAll().collectAsState(initial = emptyList())
    var week by remember { mutableStateOf<List<Pair<String, List<DayRecord>>>>(emptyList()) }
    var streaks by remember { mutableStateOf(History.Streaks(0, 0, 0, 0)) }
    var month by remember { mutableStateOf<List<History.Day>>(emptyList()) }

    LaunchedEffect(Unit) {
        week = History.lastDays(db, 7)
        streaks = History.streaks(db)
        month = History.days(db, 28)
    }

    val names = limits.associate { it.packageName to it.appName }
    val maxDayMs = week.maxOfOrNull { (_, recs) -> recs.sumOf { it.usedMs } }?.coerceAtLeast(1L) ?: 1L

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Space.screen, vertical = Space.xs),
        verticalArrangement = Arrangement.spacedBy(Space.m)
    ) {
        item { BackButton(onBack) }
        item { ScreenTitle("Tu historial") }
        item { StreakCard(streaks, month, actionLabel = null, onAction = {}) }

        // ── Últimas 4 semanas ──
        item {
            SpCard {
                Text("Últimas 4 semanas", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.m))
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    month.chunked(7).forEach { WeekStrip(it) }
                }
                Spacer(Modifier.height(Space.m))
                Legend()
            }
        }

        // ── Récords ──
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Record("${streaks.bestNoKey}", "días seguidos sin pedir llave", Modifier.weight(1f))
                Record("${streaks.bestNoLimit}", "días seguidos sin llegar al límite", Modifier.weight(1f))
            }
        }

        // ── Insignias ──
        item {
            SpCard {
                Text("Insignias", style = MaterialTheme.typography.titleMedium)
                Text("Una por cada meta de días seguidos sin pedir llave.", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
                Spacer(Modifier.height(Space.m))
                Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    History.GOALS.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { Medal(it, earned = streaks.bestNoKey >= it) }
                            repeat(4 - row.size) { Spacer(Modifier.width(Sizes.medal)) }
                        }
                    }
                }
            }
        }

        item { Text("Día a día", style = MaterialTheme.typography.titleMedium) }

        if (week.all { it.second.isEmpty() }) {
            item {
                Text(
                    "Aún no hay datos. ScreenPact apunta cada día cuánto usas tus apps con límite; vuelve mañana.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sp.TextoSuave
                )
            }
        } else {
            items(week.reversed(), key = { it.first }) { (day, recs) ->
                val used = recs.sumOf { it.usedMs }
                val unlocks = recs.sumOf { it.unlocks }
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(dayLabel(day), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            formatMinutes(used) + if (unlocks > 0) " · $unlocks ${if (unlocks == 1) "llave" else "llaves"}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (unlocks > 0) Sp.Llave else Sp.TextoSuave
                        )
                    }
                    Spacer(Modifier.height(Space.xs))
                    UsageBar(used.toFloat() / maxDayMs, if (unlocks > 0) Sp.Llave else Sp.Niebla)
                    if (recs.isNotEmpty()) {
                        Spacer(Modifier.height(Space.xxs))
                        Text(
                            recs.sortedByDescending { it.usedMs }.joinToString(" · ") { "${names[it.packageName] ?: it.packageName} ${formatMinutes(it.usedMs)}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Sp.TextoSuave
                        )
                    }
                }
            }
        }
    }
}

private fun formatMinutes(ms: Long): String {
    val min = (ms / 60_000L).toInt()
    return if (min < 60) "$min min" else "${min / 60} h ${min % 60} min"
}

private fun dayLabel(day: String): String {
    if (day == History.dayKey()) return "Hoy"
    if (day == History.daysAgo(1)) return "Ayer"
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day) ?: return day
    return SimpleDateFormat("EEEE d", Locale("es", "ES")).format(date).replaceFirstChar { it.uppercase() }
}

/** Una cifra de récord en su tarjeta. */
@Composable
private fun Record(value: String, label: String, modifier: Modifier) {
    SpCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineLarge, color = Sp.Pacto)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
    }
}

/** Qué significa cada círculo del calendario. */
@Composable
private fun Legend() {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
        LegendRow(Icons.Default.Lock, Sp.Pacto, "Verde: sin llegar al límite")
        LegendRow(Icons.Outlined.Lock, Sp.Pacto, "Borde verde: llegaste al límite y aguantaste")
        LegendRow(Icons.Default.Key, Sp.Llave, "Llave: ese día pediste una llave")
    }
}

@Composable
private fun LegendRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(Sizes.iconS))
        Spacer(Modifier.width(Space.xs))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
    }
}
