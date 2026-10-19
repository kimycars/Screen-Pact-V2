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
import java.text.SimpleDateFormat
import java.util.Locale

/** Historial de los últimos 7 días y racha (Pro). */
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val limits by db.appLimitDao().observeAll().collectAsState(initial = emptyList())
    var week by remember { mutableStateOf<List<Pair<String, List<DayRecord>>>>(emptyList()) }
    var streak by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        week = History.lastDays(db, 7)
        streak = History.streak(db)
    }

    val names = limits.associate { it.packageName to it.appName }
    val maxDayMs = week.maxOfOrNull { (_, recs) -> recs.sumOf { it.usedMs } }?.coerceAtLeast(1L) ?: 1L

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Space.screen, vertical = Space.xs),
        verticalArrangement = Arrangement.spacedBy(Space.m)
    ) {
        item { BackButton(onBack) }
        item { ScreenTitle("Tu semana") }
        item {
            SpCard {
                Text(
                    if (streak == 1) "1 día sin pedir llave" else "$streak días sin pedir llave",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Sp.Llave
                )
                Text("Cada día que no usas una llave suma uno. Usar una llave la pone a cero.", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
            }
        }

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
