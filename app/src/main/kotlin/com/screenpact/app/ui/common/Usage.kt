package com.screenpact.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.screenpact.app.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Uso de hoy por app (ms), refrescado cada pocos segundos mientras la pantalla está abierta. */
@Composable
fun rememberTodayUsage(): Map<String, Long> {
    val context = LocalContext.current
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    LaunchedEffect(Unit) {
        while (true) {
            usage = withContext(Dispatchers.Default) { UsageStatsHelper.foregroundTimeTodayMs(context) }
            delay(5_000)
        }
    }
    return usage
}

/** "12 min", "2 h", "1 h 13". Única forma de escribir minutos en la app. */
fun formatMinutes(min: Int): String = when {
    min < 60 -> "$min min"
    min % 60 == 0 -> "${min / 60} h"
    else -> "${min / 60} h ${min % 60}"
}

/** "3 de octubre". */
fun formatDay(millis: Long): String =
    java.text.SimpleDateFormat("d 'de' MMMM", java.util.Locale("es", "ES")).format(java.util.Date(millis))
