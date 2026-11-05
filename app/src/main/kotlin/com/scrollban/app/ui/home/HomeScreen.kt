package com.scrollban.app.ui.home

import com.scrollban.app.Brand
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scrollban.app.data.FriendCodes
import com.scrollban.app.ui.common.PermissionCards
import com.scrollban.app.ui.common.rememberPermissionState
import com.scrollban.app.data.db.AppDatabase
import com.scrollban.app.data.db.AppLimit
import com.scrollban.app.ui.components.Avatar
import com.scrollban.app.ui.components.PrimaryButton
import com.scrollban.app.ui.components.ScreenTitle
import com.scrollban.app.ui.components.SbCard
import com.scrollban.app.ui.components.UsageBar
import com.scrollban.app.ui.theme.Sb
import com.scrollban.app.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.scrollban.app.ui.theme.Space
import com.scrollban.app.data.History
import com.scrollban.app.data.TimeWon
import com.scrollban.app.ui.common.TimeWonCard
import com.scrollban.app.data.rememberIsPro
import com.scrollban.app.ui.pro.ProFeature
import com.scrollban.app.ui.common.Invite
import com.scrollban.app.ui.components.ChipButton
import com.scrollban.app.ui.components.IconAction
import com.scrollban.app.ui.components.OutlineButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.scrollban.app.data.db.Friend
import com.scrollban.app.ui.common.AppIcon
import com.scrollban.app.ui.common.StreakCard
import com.scrollban.app.ui.common.formatMinutes
import com.scrollban.app.ui.common.formatDay
import com.scrollban.app.ui.common.rememberTodayUsage
import com.scrollban.app.ui.components.UsageRing
import com.scrollban.app.ui.theme.Sizes

@Composable
fun HomeScreen(onPair: () -> Unit, onAddApps: () -> Unit, onOpenSettings: () -> Unit, onOpenHistory: () -> Unit, onOpenPro: (ProFeature) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val limits by db.appLimitDao().observeAll().collectAsState(initial = null)
    val friends by db.friendDao().observeAll().collectAsState(initial = null)
    val usage = rememberTodayUsage()
    val permissions = rememberPermissionState()
    val isPro = rememberIsPro()
    var streaks by remember { mutableStateOf(History.Streaks(0, 0, 0, 0)) }
    var week by remember { mutableStateOf<List<History.Day>>(emptyList()) }
    var timeWon by remember { mutableStateOf<TimeWon.Summary?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            streaks = History.streaks(db)
            week = History.days(db, 7)
            timeWon = TimeWon.summary(db)
            delay(30_000)
        }
    }


    // Mientras Room no ha respondido no se pinta nada, para no enseñar un estado vacío falso.
    val l = limits ?: return
    val f = friends ?: return

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.l)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle("Hoy", modifier = Modifier.weight(1f))
                IconAction(Icons.Default.Settings, "Ajustes", onOpenSettings, tint = Sb.Niebla)
            }
        }

        if (!permissions.requiredOk) {
            item {
                Column {
                    Text("${Brand.NAME} está parado", style = MaterialTheme.typography.titleLarge, color = Sb.Aviso)
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "Falta un permiso, así que ahora mismo no cuenta tus minutos ni te bloquea.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Sb.TextoSuave
                    )
                    Spacer(Modifier.height(Space.m))
                    PermissionCards(permissions, onlyMissing = true)
                }
            }
        }

        // Sin pacto, lo primero es sellarlo; con pacto, la tarjeta del pacto va al final.
        val keeper = f.sortedBy { it.addedAt }.firstOrNull { FriendCodes.canUnlock(it, f) }
        if (keeper == null) item { NoPactCard(onPair) { Invite.share(context) } }

        if (l.isNotEmpty()) {
            timeWon?.let { tw -> item { TimeWonCard(tw) } }
            item {
                StreakCard(
                    streaks = streaks,
                    week = week,
                    actionLabel = if (isPro) "Historial" else "Ver más",
                    onAction = { if (isPro) onOpenHistory() else onOpenPro(ProFeature.HISTORY) }
                )
            }
        }

        if (l.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = Space.xl)) {
                    Text("Sin apps vigiladas", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "Elige las apps que te roban tiempo y cuántos minutos al día les das.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Sb.TextoSuave
                    )
                    Spacer(Modifier.height(Space.l))
                    PrimaryButton("Añadir apps", onAddApps)
                }
            }
        } else {
            item { TodayCard(l, usage) }
        }

        if (keeper != null) item { PactCard(keeper, f.count { it.id != keeper.id }) }
    
    }
}

private fun fractionOf(limit: AppLimit, usage: Map<String, Long>): Float {
    if (!limit.enabled || limit.dailyLimitMinutes <= 0) return -1f
    return (usage[limit.packageName] ?: 0L) / (limit.dailyLimitMinutes * 60_000f)
}

@Composable
private fun UsageRow(limit: AppLimit, usedMs: Long) {
    val usedMin = (usedMs / 60_000L).toInt()
    val fraction = if (limit.dailyLimitMinutes > 0) usedMin.toFloat() / limit.dailyLimitMinutes else 0f
    val hot = limit.enabled && fraction >= 0.8f
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(limit.packageName, size = Sizes.iconL)
            Spacer(Modifier.width(Space.s))
            Text(limit.appName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                if (limit.enabled) "$usedMin / ${limit.dailyLimitMinutes} min" else "En pausa",
                style = MaterialTheme.typography.bodyMedium,
                color = if (hot) Sb.Llave else Sb.TextoSuave
            )
        }
        Spacer(Modifier.height(Space.xs))
        UsageBar(fraction, if (hot) Sb.Llave else Sb.Niebla, track = Sb.Linea)
    }
}


@Composable
private fun NoPactCard(onPair: () -> Unit, onInvite: () -> Unit) {
    SbCard {
        Text("Aún no tienes pacto", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Space.xxs))
        Text(
            "Sin un amigo con tu llave, nadie podrá desbloquearte. Queda con alguien y escanead vuestros QR.",
            style = MaterialTheme.typography.bodySmall,
            color = Sb.TextoSuave
        )
        Spacer(Modifier.height(Space.m))
        PrimaryButton("Sellar un pacto", onPair)
        Spacer(Modifier.height(Space.xs))
        OutlineButton("Invitar a un amigo", onInvite)
    }
}

@Composable
private fun PactCard(keeper: Friend, others: Int) {
    SbCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(keeper.name)
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(
                    if (others == 0) "${keeper.name} tiene tu llave" else "${keeper.name} y $others más tienen tu llave",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Pacto desde el ${formatDay(keeper.addedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sb.TextoSuave
                )
            }
        }
    }
}

/**
 * Uso de hoy. Cada sitio dice una cosa: el anillo, lo que te queda en total; la frase de al
 * lado, cómo vas (sin números); cada fila, lo usado de esa app frente a su límite.
 */
@Composable
private fun TodayCard(limits: List<AppLimit>, usage: Map<String, Long>) {
    val active = limits.filter { it.enabled && it.dailyLimitMinutes > 0 }
    val usedOf = { l: AppLimit -> ((usage[l.packageName] ?: 0L) / 60_000L).toInt() }
    val usedMin = active.sumOf(usedOf)
    val limitMin = active.sumOf { it.dailyLimitMinutes }
    val leftMin = (limitMin - usedMin).coerceAtLeast(0)
    val fraction = if (limitMin > 0) usedMin.toFloat() / limitMin else 0f
    val closed = active.filter { usedOf(it) >= it.dailyLimitMinutes }
    val close = active.filter { it !in closed && usedOf(it) >= it.dailyLimitMinutes * 0.8f }
    val status = when {
        active.isEmpty() -> "Todas tus apps están en pausa."
        closed.size == active.size -> "Hoy ya has gastado todo tu tiempo. Mañana más."
        closed.isNotEmpty() -> "${closed.joinToString(" y ") { it.appName }} ${if (closed.size == 1) "está cerrada" else "están cerradas"} hasta mañana."
        close.isNotEmpty() -> "${close.first().appName} está cerca de su límite."
        else -> "Vas bien. Sigue así."
    }
    SbCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UsageRing(fraction, if (fraction >= 0.8f) Sb.Llave else Sb.Pacto) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatMinutes(leftMin), style = MaterialTheme.typography.titleLarge)
                    Text("te quedan", style = MaterialTheme.typography.labelSmall, color = Sb.TextoSuave)
                }
            }
            Spacer(Modifier.width(Space.l))
            Text(status, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        limits.sortedByDescending { fractionOf(it, usage) }.forEach { app ->
            Spacer(Modifier.height(Space.l))
            UsageRow(app, usage[app.packageName] ?: 0L)
        }
    }
}
