package com.screenpact.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screenpact.app.ui.components.CodeInput
import com.screenpact.app.ui.components.PrimaryButton
import com.screenpact.app.ui.theme.ScreenPactTheme
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space

/**
 * Pantalla de bloqueo: el elemento firma de la marca. Amarilla a pantalla completa y con el
 * nombre del amigo como protagonista, en lugar de una pantalla roja que regaña.
 */
@Composable
fun BlockedOverlayContent(
    appName: String,
    usedMinutes: Int,
    limitMinutes: Int,
    tamperReason: String?,
    scheduleText: String?,
    friendNames: List<String>,
    errorMessage: String?,
    onSubmit: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    // Tras un intento fallido se vacían las casillas para escribir la llave nueva.
    LaunchedEffect(errorMessage) { if (errorMessage != null) code = "" }

    ScreenPactTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(Sp.Llave)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.xl)
                .padding(top = Space.hero, bottom = Space.xxl)
        ) {
            if (tamperReason != null) {
                Text("$appName · pacto en pausa", style = MaterialTheme.typography.titleMedium, color = Sp.Tinta)
                Spacer(Modifier.height(Space.m))
                Text("Algo ha tocado tu pacto.", style = MaterialTheme.typography.displayLarge, color = Sp.Tinta)
                Spacer(Modifier.height(Space.m))
                Text(tamperReason, style = MaterialTheme.typography.bodyLarge, color = Sp.Tinta)
            } else if (scheduleText != null) {
                Text("$appName · horario", style = MaterialTheme.typography.titleMedium, color = Sp.Tinta)
                Spacer(Modifier.height(Space.m))
                Text("$appName está cerrada ahora.", style = MaterialTheme.typography.displayLarge, color = Sp.Tinta)
                Spacer(Modifier.height(Space.m))
                Text("Tu horario la cierra $scheduleText.", style = MaterialTheme.typography.bodyLarge, color = Sp.Tinta)
            } else {
                Text("$appName · $usedMinutes de $limitMinutes min", style = MaterialTheme.typography.titleMedium, color = Sp.Tinta)
                Spacer(Modifier.height(Space.m))
                Text("Se acabó $appName por hoy.", style = MaterialTheme.typography.displayLarge, color = Sp.Tinta)
            }

            Spacer(Modifier.height(Space.huge))
            Text("¿De verdad lo necesitas?", style = MaterialTheme.typography.bodyLarge, color = Sp.Tinta)
            Text(
                when (friendNames.size) {
                    0 -> "Pídele la llave a un amigo"
                    1 -> "Pídele la llave a ${friendNames[0]}"
                    else -> "Pídele la llave a ${friendNames[0]} o a ${friendNames[1]}"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Sp.Tinta
            )

            Spacer(Modifier.height(Space.xl))
            Text("Llave de 6 cifras", style = MaterialTheme.typography.labelMedium, color = Sp.Tinta)
            Spacer(Modifier.height(Space.xs))
            CodeInput(code = code, onCodeChange = { code = it }, onYellow = true, autoFocus = false)
            if (errorMessage != null) {
                Spacer(Modifier.height(Space.s))
                Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = Sp.Tinta)
            }

            Spacer(Modifier.height(Space.xl))
            PrimaryButton(
                "Abrir 5 minutos",
                { if (code.length == 6) onSubmit(code) },
                enabled = code.length == 6,
                container = Sp.Tinta,
                content = Sp.Llave
            )
        }
    }
}
