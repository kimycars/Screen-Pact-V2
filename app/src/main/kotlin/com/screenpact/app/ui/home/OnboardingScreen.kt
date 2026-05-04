package com.screenpact.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenpact.app.data.Prefs
import com.screenpact.app.services.UsageMonitorService
import com.screenpact.app.util.PermissionsHelper
import com.screenpact.app.util.UsageStatsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var hasUsage by remember { mutableStateOf(UsageStatsHelper.hasUsageAccess(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionsHelper.canDrawOverlays(context)) }

    LaunchedEffect(Unit) {
        name = Prefs.getOwnName(context)
    }

    // Re-evalúa permisos cuando se recompone (al volver de Ajustes).
    LaunchedEffect(Unit) {
        while (true) {
            delay(800)
            hasUsage = UsageStatsHelper.hasUsageAccess(context)
            hasOverlay = PermissionsHelper.canDrawOverlays(context)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("ScreenPact", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Tú solo no puedes desbloquearte.", fontSize = 16.sp)
        Spacer(Modifier.height(24.dp))

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("1. Tu nombre", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cómo te verán tus amigos") },
                    singleLine = true
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        PermissionRow(
            title = "2. Acceso a uso de apps",
            granted = hasUsage,
            onAction = { PermissionsHelper.openUsageAccessSettings(context) }
        )
        Spacer(Modifier.height(8.dp))
        PermissionRow(
            title = "3. Dibujar sobre otras apps",
            granted = hasOverlay,
            onAction = { PermissionsHelper.openOverlaySettings(context) }
        )
        Spacer(Modifier.height(8.dp))
        PermissionRow(
            title = "4. Ignorar optimización de batería (recomendado)",
            granted = false,
            ctaText = "Abrir ajustes",
            optional = true,
            onAction = { PermissionsHelper.openBatteryOptimizationSettings(context) }
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                scope.launch {
                    Prefs.setOwnName(context, name.trim().ifEmpty { "Yo" })
                    if (hasUsage && hasOverlay) UsageMonitorService.start(context)
                    onContinue()
                }
            },
            enabled = name.isNotBlank() && hasUsage && hasOverlay,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continuar") }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    ctaText: String = "Conceder",
    optional: Boolean = false,
    onAction: () -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    granted -> "Concedido"
                    optional -> "Opcional, mejora la fiabilidad en background"
                    else -> "Requerido"
                },
                fontSize = 12.sp
            )
            if (!granted) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onAction) { Text(ctaText) }
            }
        }
    }
}
