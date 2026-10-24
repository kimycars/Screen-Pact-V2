package com.screenpact.app.ui.home

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
import com.screenpact.app.data.FriendCodes
import com.screenpact.app.ui.common.PermissionCards
import com.screenpact.app.ui.common.rememberPermissionState
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.AppLimit
import com.screenpact.app.ui.components.Avatar
import com.screenpact.app.ui.components.PrimaryButton
import com.screenpact.app.ui.components.ScreenTitle
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.UsageBar
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.data.History
import com.screenpact.app.data.rememberIsPro
import com.screenpact.app.ui.pro.ProFeature
import com.screenpact.app.ui.common.Invite
import com.screenpact.app.ui.components.ChipButton
import com.screenpact.app.ui.components.IconAction
import com.screenpact.app.ui.components.OutlineButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.screenpact.app.data.db.Friend
import com.screenpact.app.ui.common.AppIcon
import com.screenpact.app.ui.common.StreakCard
import com.screenpact.app.ui.components.UsageRing
import com.screenpact.app.ui.theme.Sizes

@Composable
fun HomeScreen(onPair: () -> Unit, onAddApps: () -> Unit, onOpenSettings: () -> Unit, onOpenHistory: () -> Unit, onOpenPro: (ProFeature) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val limits by db.appLimitDao().observeAll().collectAsState(initial = null)
    val friends by db.friendDao().observeAll().collectAsState(initial = null)
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    val permissions = rememberPermissionState()
    val isPro = rememberIsPro()
    var streaks by remember { mutableStateOf(History.Streaks(0, 0, 0, 0)) }
    var week by remember { mutableStateOf<List<History.Day>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            streaks = History.streaks(db)
            week = History.days(db, 7)
            delay(30_000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            usage = withContext(Dispatchers.Default) { UsageStatsHelper.foregroundTimeTodayMs(context) }
            delay(5_000)
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
                IconAction(Icons.Default.Settings, "Ajustes", onOpenSettings, tint = Sp.Niebla)
            }
        }

        if (!permissions.requiredOk) {
            item {
                Column {
                    Text("ScreenPact está parado", style = MaterialTheme.typography.titleLarge, color = Sp.Aviso)
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "Falta un permiso, así que ahora mismo no cuenta tus minutos ni te bloquea.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Sp.TextoSuave
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
                        color = Sp.TextoSuave
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
    val left = limit.dailyLimitMinutes - usedMin
    val hot = limit.enabled && fraction >= 0.8f
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(limit.packageName, size = Sizes.iconL)
            Spacer(Modifier.width(Space.s))
            Text(limit.appName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                if (limit.enabled) "$usedMin de ${limit.dailyLimitMinutes} min" else "En pausa",
                style = MaterialTheme.typography.bodyMedium,
                color = if (hot) Sp.Llave else Sp.TextoSuave
            )
        }
        Spacer(Modifier.height(Space.xs))
        UsageBar(fraction, if (hot) Sp.Llave else Sp.Niebla, track = Sp.Linea)
        if (limit.enabled && hot) {
            Spacer(Modifier.height(Space.xs))
            Text(
                when {
                    left <= 0 -> "Se acabó por hoy"
                    left == 1 -> "Te queda 1 minuto"
                    else -> "Te quedan $left minutos"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Sp.TextoSuave
            )
        }
    }
}

private fun formatDay(millis: Long): String =
    SimpleDateFormat("d 'de' MMMM", Locale("es", "ES")).format(Date(millis))

@Composable
private fun NoPactCard(onPair: () -> Unit, onInvite: () -> Unit) {
    SpCard {
        Text("Aún no tienes pacto", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Space.xxs))
        Text(
            "Sin un amigo con tu llave, nadie podrá desbloquearte. Queda con alguien y escanead vuestros QR.",
            style = MaterialTheme.typography.bodySmall,
            color = Sp.TextoSuave
        )
        Spacer(Modifier.height(Space.m))
        PrimaryButton("Sellar un pacto", onPair)
        Spacer(Modifier.height(Space.xs))
        OutlineButton("Invitar a un amigo", onInvite)
    }
}

@Composable
private fun PactCard(keeper: Friend, others: Int) {
    SpCard {
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
                    color = Sp.TextoSuave
                )
            }
        }
    }
}

/** Uso de hoy: anillo con el total de las apps con límite y una fila por app, la más apurada arriba. */
@Composable
private fun TodayCard(limits: List<AppLimit>, usage: Map<String, Long>) {
    val active = limits.filter { it.enabled && it.dailyLimitMinutes > 0 }
    val usedMin = active.sumOf { ((usage[it.packageName] ?: 0L) / 60_000L).toInt() }
    val limitMin = active.sumOf { it.dailyLimitMinutes }
    val fraction = if (limitMin > 0) usedMin.toFloat() / limitMin else 0f
    SpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UsageRing(fraction, if (fraction >= 0.8f) Sp.Llave else Sp.Pacto) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatMinutes(usedMin), style = MaterialTheme.typography.titleLarge)
                    Text("hoy", style = MaterialTheme.typography.labelSmall, color = Sp.TextoSuave)
                }
            }
            Spacer(Modifier.width(Space.l))
            Column(Modifier.weight(1f)) {
                Text("De ${formatMinutes(limitMin)} que te das", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        limitMin == 0 -> "Todas tus apps están en pausa."
                        usedMin >= limitMin -> "Hoy ya has gastado todo tu tiempo."
                        else -> "Te quedan ${formatMinutes(limitMin - usedMin)} en total."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Sp.TextoSuave
                )
            }
        }
        limits.sortedByDescending { fractionOf(it, usage) }.forEach { app ->
            Spacer(Modifier.height(Space.l))
            UsageRow(app, usage[app.packageName] ?: 0L)
        }
    }
}

private fun formatMinutes(min: Int): String = when {
    min < 60 -> "$min min"
    min % 60 == 0 -> "${min / 60} h"
    else -> "${min / 60} h ${min % 60}"
}
