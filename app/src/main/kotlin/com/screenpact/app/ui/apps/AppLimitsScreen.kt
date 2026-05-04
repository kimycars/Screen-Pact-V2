package com.screenpact.app.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.AppLimit
import com.screenpact.app.util.AppListHelper
import com.screenpact.app.util.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val limits by db.appLimitDao().observeAll().collectAsState(initial = emptyList())

    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apps & límites") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Añadir app")
            }
            Spacer(Modifier.height(16.dp))
            if (limits.isEmpty()) {
                Text("Sin apps. Añade una para empezar a monitorizar.", fontSize = 14.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(limits, key = { it.packageName }) { l ->
                        LimitCard(
                            limit = l,
                            onToggle = { enabled ->
                                scope.launch { db.appLimitDao().upsert(l.copy(enabled = enabled)) }
                            },
                            onChange = { newMin ->
                                scope.launch { db.appLimitDao().upsert(l.copy(dailyLimitMinutes = newMin)) }
                            },
                            onDelete = { scope.launch { db.appLimitDao().delete(l.packageName) } }
                        )
                    }
                }
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            existing = limits.map { it.packageName }.toSet(),
            onDismiss = { showPicker = false },
            onPick = { app ->
                showPicker = false
                scope.launch {
                    db.appLimitDao().upsert(
                        AppLimit(
                            packageName = app.packageName,
                            appName = app.label,
                            dailyLimitMinutes = 30,
                            enabled = true
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun LimitCard(
    limit: AppLimit,
    onToggle: (Boolean) -> Unit,
    onChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(limit) { mutableStateOf(limit.dailyLimitMinutes.toString()) }

    Card {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(limit.appName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = limit.enabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null) }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (editing) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { v -> if (v.all { it.isDigit() } && v.length <= 4) draft = v },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Minutos al día") }
                    )
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = {
                        val n = draft.toIntOrNull() ?: limit.dailyLimitMinutes
                        onChange(n.coerceIn(1, 1440))
                        editing = false
                    }) { Text("OK") }
                } else {
                    Text("${limit.dailyLimitMinutes} min/día", modifier = Modifier.weight(1f))
                    TextButton(onClick = { editing = true }) { Text("Cambiar") }
                }
            }
        }
    }
}

@Composable
private fun AppPickerDialog(
    existing: Set<String>,
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { AppListHelper.listLaunchable(context) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Elige app") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                val filtered = remember(apps, query, existing) {
                    apps.filter { it.packageName !in existing && it.label.contains(query, ignoreCase = true) }
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(app) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bmp = remember(app.packageName) {
                                runCatching { app.icon?.toBitmap(64, 64) }.getOrNull()
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                            }
                            Text(app.label)
                        }
                    }
                }
            }
        }
    )
}
