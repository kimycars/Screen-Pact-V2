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
import com.screenpact.app.ui.common.Invite
import com.screenpact.app.ui.components.ChipButton
import com.screenpact.app.ui.components.IconAction
import com.screenpact.app.ui.components.OutlineButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings

@Composable
fun HomeScreen(onPair: () -> Unit, onAddApps: () -> Unit, onOpenSettings: () -> Unit, onOpenHistory: () -> Unit, onOpenPro: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val limits by db.appLimitDao().observeAll().collectAsState(initial = null)
    val friends by db.friendDao().observeAll().collectAsState(initial = null)
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    val permissions = rememberPermissionState()
    val isPro = rememberIsPro()
    var streak by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            streak = History.streak(db)
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

        item {
            // El amigo que puede desbloquearte ya (el más antiguo con poder de llave).
            val keeper = f.sortedBy { it.addedAt }.firstOrNull { FriendCodes.canUnlock(it, f) }
            if (keeper == null) {
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
                    OutlineButton("Invitar a un amigo", { Invite.share(context) })
                }
            } else {
                val others = f.count { it.id != keeper.id }
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
        }

        if (l.isNotEmpty()) {
            item {
                SpCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            if (isPro) {
                                Text(
                                    if (streak == 1) "1 día sin pedir llave" else "$streak días sin pedir llave",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text("Tu racha. Se rompe si hoy usas una llave.", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
                            } else {
                                Text("Rachas e historial", style = MaterialTheme.typography.titleMedium)
                                Text("Mira tu semana y cuántos días llevas sin pedir llave.", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
                            }
                        }
                        Spacer(Modifier.width(Space.s))
                        if (isPro) ChipButton("Historial", onOpenHistory) else ChipButton("Ver Pro", onOpenPro)
                    }
                }
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
            // Las más cerca de su límite, arriba.
            val sorted = l.sortedByDescending { fractionOf(it, usage) }
            items(sorted, key = { it.packageName }) { app -> UsageRow(app, usage[app.packageName] ?: 0L) }
        }
    
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
        Row(Modifier.fillMaxWidth()) {
            Text(limit.appName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                if (limit.enabled) "$usedMin de ${limit.dailyLimitMinutes} min" else "En pausa",
                style = MaterialTheme.typography.bodyMedium,
                color = if (hot) Sp.Llave else Sp.TextoSuave
            )
        }
        Spacer(Modifier.height(Space.xs))
        UsageBar(fraction, if (hot) Sp.Llave else Sp.Niebla)
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
