package com.screenpact.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.util.UsageStatsHelper
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(onOpenFriends: () -> Unit, onOpenLimits: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val limits by db.appLimitDao().observeAll().collectAsState(initial = emptyList())
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    LaunchedEffect(Unit) {
        while (true) {
            usage = UsageStatsHelper.foregroundTimeTodayMs(context)
            delay(5_000)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("ScreenPact", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenFriends, modifier = Modifier.weight(1f)) { Text("Amigos") }
            OutlinedButton(onClick = onOpenLimits, modifier = Modifier.weight(1f)) { Text("Apps & límites") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Hoy", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        if (limits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No has añadido apps. Ve a 'Apps & límites' para empezar.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(limits, key = { it.packageName }) { l ->
                    val usedMs = usage[l.packageName] ?: 0L
                    val usedMin = (usedMs / 60_000).toInt()
                    val pct = if (l.dailyLimitMinutes > 0) (usedMin.toFloat() / l.dailyLimitMinutes).coerceIn(0f, 1f) else 0f
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(l.appName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("$usedMin / ${l.dailyLimitMinutes} min", fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
