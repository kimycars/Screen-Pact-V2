package com.screenpact.app.ui.apps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.AppLimit
import com.screenpact.app.ui.common.FriendCodeDialog
import com.screenpact.app.ui.components.CardButton
import com.screenpact.app.ui.components.IconAction
import com.screenpact.app.ui.components.ChipButton
import com.screenpact.app.ui.components.QuietButton
import com.screenpact.app.ui.components.ScreenTitle
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.SpTextField
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.util.AppListHelper
import com.screenpact.app.util.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.data.Stats
import com.screenpact.app.data.Plans
import com.screenpact.app.data.Schedules
import com.screenpact.app.data.rememberIsPro
import com.screenpact.app.ui.pro.ProFeature
import com.screenpact.app.ui.common.ScheduleDialog

/**
 * Reglas de edición (el objetivo es que solo se pueda endurecer sin ayuda):
 *  - Sin llave: añadir apps, bajar un límite, reactivar una app en pausa.
 *  - Con la llave de un amigo: quitar una app, subir un límite, pausar una app.
 *  - Horarios (Pro): ampliarlos es libre; acortarlos o quitarlos pide llave.
 *  - Plan gratis: hasta Plans.FREE_APP_LIMIT apps; la siguiente lleva a Pro.
 * Si aún no hay amigos emparejados todo es libre: no hay a quién pedir la llave.
 */
@Composable
fun AppLimitsScreen(onOpenPro: (ProFeature) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val limits by db.appLimitDao().observeAll().collectAsState(initial = emptyList())
    val friends by db.friendDao().observeAll().collectAsState(initial = emptyList())

    var showPicker by remember { mutableStateOf(false) }
    var pickedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var editing by remember { mutableStateOf<AppLimit?>(null) }
    var options by remember { mutableStateOf<AppLimit?>(null) }
    var scheduling by remember { mutableStateOf<AppLimit?>(null) }
    val isPro = rememberIsPro()
    val atFreeLimit = !isPro && limits.size >= Plans.FREE_APP_LIMIT
    // Acción que afloja el control, a la espera de que un amigo dé su llave.
    var pending by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    fun loosen(reason: String, action: () -> Unit) {
        if (friends.isEmpty()) action() else pending = reason to action
    }

    fun save(limit: AppLimit) {
        scope.launch { db.appLimitDao().upsert(limit) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.s)
    ) {
        item { ScreenTitle("Límites") }
        item {
            Row(Modifier.padding(top = Space.xs, bottom = Space.xs)) {
                Icon(Icons.Default.Lock, null, tint = Sp.Llave, modifier = Modifier.size(Sizes.iconM))
                Spacer(Modifier.width(Space.s))
                Text(
                    "Bajar un límite o añadir apps es libre. Subirlo, pausarlo o quitar una app pide la llave de un amigo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sp.TextoSuave
                )
            }
        }

        items(limits, key = { it.packageName }) { l ->
            LimitRow(
                limit = l,
                onChange = { editing = l },
                onOptions = { options = l },
                onResume = { save(l.copy(enabled = true)) },
                onSchedule = { if (isPro) scheduling = l else onOpenPro(ProFeature.SCHEDULE) }
            )
        }

        item {
            CardButton(
                when {
                    limits.isEmpty() -> "Añadir tu primera app"
                    atFreeLimit -> "Añadir otra app con Pro"
                    else -> "Añadir otra app"
                },
                { if (atFreeLimit) onOpenPro(ProFeature.APPS) else showPicker = true }
            )
        }
        if (!isPro) {
            item {
                QuietButton(
                    "Horarios, historial y más con ScreenPact Pro",
                    { onOpenPro(ProFeature.GENERAL) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    editing?.let { l ->
        MinutesDialog(
            title = l.appName,
            message = "Puedes bajarlo cuando quieras. Para subirlo hará falta la llave de un amigo.",
            initial = l.dailyLimitMinutes,
            confirm = "Guardar",
            onDismiss = { editing = null },
            onConfirm = { newMin ->
                editing = null
                if (newMin <= l.dailyLimitMinutes) save(l.copy(dailyLimitMinutes = newMin))
                else loosen("Para subir ${l.appName} a $newMin min al día necesitas la llave de un amigo.") {
                    save(l.copy(dailyLimitMinutes = newMin))
                }
            }
        )
    }

    options?.let { l ->
        AlertDialog(
            onDismissRequest = { options = null },
            title = { Text(l.appName) },
            text = { Text("Estas dos acciones aflojan tu pacto, así que piden la llave de un amigo.") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (l.enabled) TextButton(onClick = {
                        options = null
                        loosen("Para pausar el límite de ${l.appName} necesitas la llave de un amigo.") { save(l.copy(enabled = false)) }
                    }) { Text("Pausar el límite") }
                    TextButton(onClick = {
                        options = null
                        loosen("Para quitar ${l.appName} de la lista necesitas la llave de un amigo.") {
                            scope.launch { db.appLimitDao().delete(l.packageName) }
                        }
                    }) { Text("Quitar de la lista", color = Sp.Aviso) }
                    TextButton(onClick = { options = null }) { Text("Cancelar", color = Sp.TextoSuave) }
                }
            }
        )
    }

    scheduling?.let { l ->
        val oldStart = l.scheduleStart
        val oldEnd = l.scheduleEnd
        ScheduleDialog(
            appName = l.appName,
            initialStart = oldStart ?: 0,
            initialEnd = oldEnd ?: 8 * 60,
            onDismiss = { scheduling = null },
            onConfirm = { start, end ->
                scheduling = null
                val updated = l.copy(scheduleStart = start, scheduleEnd = end)
                // Ampliar (o poner por primera vez) endurece: libre. Dejar horas abiertas, pide llave.
                if (!l.hasSchedule || Schedules.covers(start, end, oldStart!!, oldEnd!!)) save(updated)
                else loosen("Acortar el horario de ${l.appName} afloja tu pacto: necesitas la llave de un amigo.") { save(updated) }
            },
            onRemove = if (l.hasSchedule) ({
                scheduling = null
                loosen("Para quitar el horario de ${l.appName} necesitas la llave de un amigo.") {
                    save(l.copy(scheduleStart = null, scheduleEnd = null))
                }
            }) else null
        )
    }

    pending?.let { (reason, action) ->
        FriendCodeDialog(
            reason = reason,
            onDismiss = { pending = null },
            onVerified = {
                pending = null
                action()
            }
        )
    }

    if (showPicker) {
        AppPickerDialog(
            existing = limits.map { it.packageName }.toSet(),
            onDismiss = { showPicker = false },
            onPick = { app ->
                showPicker = false
                pickedApp = app
            }
        )
    }

    pickedApp?.let { app ->
        MinutesDialog(
            title = app.label,
            message = "¿Cuántos minutos al día? Luego podrás bajarlo cuando quieras, pero subirlo pedirá la llave de un amigo.",
            initial = 30,
            confirm = "Añadir",
            onDismiss = { pickedApp = null },
            onConfirm = { minutes ->
                pickedApp = null
                save(AppLimit(packageName = app.packageName, appName = app.label, dailyLimitMinutes = minutes, enabled = true))
                Stats.count(context, Stats.Event.LIMIT_ADDED)
            }
        )
    }
}

@Composable
private fun LimitRow(limit: AppLimit, onChange: () -> Unit, onOptions: () -> Unit, onResume: () -> Unit, onSchedule: () -> Unit) {
    SpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(limit.appName, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (limit.enabled) "${limit.dailyLimitMinutes} min al día" else "En pausa · ${limit.dailyLimitMinutes} min al día",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sp.TextoSuave
                )
            }
            if (limit.enabled) ChipButton("Cambiar", onChange) else ChipButton("Reactivar", onResume)
            IconAction(Icons.Default.MoreVert, "Más opciones de ${limit.appName}", onOptions)
        }
        Spacer(Modifier.height(Space.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (limit.hasSchedule) "Cerrada ${Schedules.describe(limit.scheduleStart!!, limit.scheduleEnd!!)}" else "Sin horario de cierre",
                style = MaterialTheme.typography.bodySmall,
                color = if (limit.hasSchedule) Sp.Llave else Sp.TextoSuave,
                modifier = Modifier.weight(1f)
            )
            ChipButton("Horario", onSchedule)
        }
    }
}

@Composable
private fun MinutesDialog(
    title: String,
    message: String,
    initial: Int,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var draft by remember { mutableStateOf(initial.toString()) }
    val minutes = draft.toIntOrNull()?.takeIf { it in 1..1440 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, color = Sp.TextoSuave)
                Spacer(Modifier.height(Space.m))
                SpTextField(
                    value = draft,
                    onValueChange = { v -> if (v.all { it.isDigit() } && v.length <= 4) draft = v },
                    label = "Minutos al día",
                    keyboardType = KeyboardType.Number
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let(onConfirm) }, enabled = minutes != null) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Sp.TextoSuave) } }
    )
}

@Composable
private fun AppPickerDialog(
    existing: Set<String>,
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { AppListHelper.listLaunchable(context) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Sp.TextoSuave) } },
        title = { Text("Elige una app") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                SpTextField(value = query, onValueChange = { query = it }, label = "Buscar")
                Spacer(Modifier.height(Space.xs))
                val list = apps
                if (list == null) {
                    Text("Cargando tus apps…", color = Sp.TextoSuave, modifier = Modifier.padding(vertical = Space.xl))
                } else {
                    val filtered = remember(list, query, existing) {
                        list.filter { it.packageName !in existing && it.label.contains(query, ignoreCase = true) }
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(Sizes.pickerList)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(app) }
                                    .padding(vertical = Space.s),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bmp = remember(app.packageName) {
                                    runCatching { app.icon?.toBitmap(64, 64) }.getOrNull()
                                }
                                if (bmp != null) {
                                    Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(Sizes.appIcon))
                                    Spacer(Modifier.width(Space.s))
                                }
                                Text(app.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    )
}
