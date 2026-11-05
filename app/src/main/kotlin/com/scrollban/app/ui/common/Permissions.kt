package com.scrollban.app.ui.common

import com.scrollban.app.Brand
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scrollban.app.ui.components.PermissionCard
import com.scrollban.app.util.PermissionsHelper
import com.scrollban.app.util.UsageStatsHelper
import kotlinx.coroutines.delay
import com.scrollban.app.ui.theme.Space

data class PermissionState(val usage: Boolean, val overlay: Boolean, val battery: Boolean) {
    val requiredOk: Boolean get() = usage && overlay
}

/** Estado de los permisos, refrescado cada poco: al volver de Ajustes no hay callback. */
@Composable
fun rememberPermissionState(): PermissionState {
    val context = LocalContext.current
    fun read() = PermissionState(
        usage = UsageStatsHelper.hasUsageAccess(context),
        overlay = PermissionsHelper.canDrawOverlays(context),
        battery = PermissionsHelper.isBatteryUnrestricted(context)
    )
    var state by remember { mutableStateOf(read()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(800)
            state = read()
        }
    }
    return state
}

/**
 * Las tarjetas de permisos, con la ayuda para encontrar cada interruptor. Se usan en la
 * bienvenida (todas) y en Hoy (solo las que faltan, con [onlyMissing]).
 */
@Composable
fun PermissionCards(state: PermissionState, onlyMissing: Boolean = false) {
    val context = LocalContext.current
    val listHelp = "Si se abre una lista, busca ${Brand.NAME} y activa su interruptor."
    val restrictedHelp = if (PermissionsHelper.mayHaveRestrictedSettings)
        " ¿Sale gris o dice \"ajuste restringido\"? Toca \"Ficha de la app\", luego ⋮ arriba a la derecha y \"Permitir ajustes restringidos\". Después vuelve a darle a \"Dar\"."
    else ""

    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        if (!onlyMissing || !state.usage) PermissionCard(
            title = "Acceso a datos de uso",
            why = "Para contar los minutos que llevas en cada app.",
            granted = state.usage,
            onGrant = { PermissionsHelper.openUsageAccessSettings(context) },
            help = listHelp + restrictedHelp
        )
        if (!state.requiredOk && PermissionsHelper.mayHaveRestrictedSettings) PermissionCard(
            title = "Ficha de la app",
            why = "Solo si algún permiso sale gris o bloqueado.",
            granted = false,
            action = "Abrir",
            onGrant = { PermissionsHelper.openAppDetails(context) }
        )
        if (!onlyMissing || !state.overlay) PermissionCard(
            title = "Mostrar encima de otras apps",
            why = "Para enseñarte la pantalla de bloqueo cuando se acabe tu tiempo.",
            granted = state.overlay,
            onGrant = { PermissionsHelper.openOverlaySettings(context) },
            help = listHelp + restrictedHelp
        )
        if (!onlyMissing || !state.battery) PermissionCard(
            title = "Sin límite de batería (recomendado)",
            why = "Algunos móviles cierran apps en segundo plano. Así ${Brand.NAME} no se duerme.",
            granted = state.battery,
            onGrant = { PermissionsHelper.openBatteryOptimizationSettings(context) }
        )
        // Ajuste propio del fabricante. Ninguno deja saber si está activado, así que se ofrece
        // siempre en la bienvenida y nunca como "falta" en Hoy.
        val oem = PermissionsHelper.backgroundSetting
        if (!onlyMissing && oem != null) PermissionCard(
            title = "${oem.title} (recomendado)",
            why = "Tu móvil cierra apps en segundo plano. Así ${Brand.NAME} sigue contando.",
            granted = false,
            action = "Abrir",
            onGrant = { PermissionsHelper.openBackgroundSetting(context) },
            help = oem.help
        )
    }
}
