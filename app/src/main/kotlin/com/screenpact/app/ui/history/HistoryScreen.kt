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
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.ui.common.Medal
import com.screenpact.app.ui.common.StreakCard
import com.screenpact.app.ui.common.WeekStrip
import com.screenpact.app.ui.common.AppIcon
import com.screenpact.app.ui.common.dayLetter
import com.screenpact.app.ui.common.formatMinutes
import com.screenpact.app.ui.components.WeekBars
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

        item { Text("Por app, esta semana", style = MaterialTheme.typography.titleMedium) }

        // Una tarjeta por app: sus 7 días en barras, con la línea de su límite.
        val apps = week.flatMap { it.second }.map { it.packageName }.distinct()
            .sortedByDescending { pkg -> week.sumOf { (_, recs) -> recs.filter { it.packageName == pkg }.sumOf { it.usedMs } } }
        if (apps.isEmpty()) {
            item {
                Text(
                    "Aún no hay datos. ScreenPact apunta cada día cuánto usas tus apps con límite; vuelve mañana.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sp.TextoSuave
                )
            }
        } else {
            items(apps, key = { it }) { pkg ->
                val perDay = week.map { (_, recs) -> recs.firstOrNull { it.packageName == pkg } }
                AppWeekCard(
                    packageName = pkg,
                    name = names[pkg] ?: pkg,
                    perDay = perDay,
                    labels = week.map { dayLetter(it.first) }
                )
            }
        }
    }
}

/** Semana de una app: barras por día, su límite, la media y cuántos días se quedó por debajo. */
@Composable
private fun AppWeekCard(packageName: String, name: String, perDay: List<DayRecord?>, labels: List<String>) {
    val days = perDay.filterNotNull()
    val limitMs = (days.lastOrNull()?.limitMinutes ?: 0) * 60_000L
    val avgMin = if (days.isEmpty()) 0 else (days.sumOf { it.usedMs } / days.size / 60_000L).toInt()
    val under = days.count { it.limitMinutes > 0 && it.usedMs < it.limitMinutes * 60_000L }
    val keys = days.sumOf { it.unlocks }
    SpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName, size = Sizes.iconL)
            Spacer(Modifier.width(Space.s))
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (keys > 0) Text(if (keys == 1) "1 llave" else "$keys llaves", style = MaterialTheme.typography.bodySmall, color = Sp.Llave)
        }
        Spacer(Modifier.height(Space.m))
        WeekBars(values = perDay.map { it?.usedMs ?: 0L }, limit = limitMs, labels = labels)
        Spacer(Modifier.height(Space.s))
        Text(
            "Media: ${formatMinutes(avgMin)} al día · $under de ${days.size} días por debajo",
            style = MaterialTheme.typography.bodySmall,
            color = Sp.TextoSuave
        )
    }
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
