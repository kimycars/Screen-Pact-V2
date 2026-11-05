package com.screenpact.app.ui.friends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.screenpact.app.data.IosCodes
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.Friend
import com.screenpact.app.ui.components.BackButton
import com.screenpact.app.ui.components.OutlineButton
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.StepLine
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.theme.TextStyles
import kotlinx.coroutines.launch

/**
 * La "llave" de un amigo con iPhone: el código de Tiempo de uso que le pones tú en su iPhone.
 * Cambiarlo no se guarda hasta que dices que ya lo has puesto en su iPhone, para no perder el
 * que vale ahora.
 */
@Composable
fun IosCodeScreen(friend: Friend, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(friend.screenTimeCode ?: "") }
    var proposed by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.xl).padding(top = Space.xs, bottom = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) { BackButton(onBack) }
        Spacer(Modifier.height(Space.xxl))
        Text("Código de Tiempo de uso de", style = MaterialTheme.typography.bodyMedium, color = Sp.TextoSuave)
        Text(friend.name, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Space.xl))
        Text(
            code.toCharArray().joinToString(" "),
            style = TextStyles.keyLarge,
            color = Sp.Llave,
            modifier = Modifier.semantics { contentDescription = "Código: ${code.toCharArray().joinToString(" ")}" }
        )
        Spacer(Modifier.height(Space.xl))
        SpCard {
            StepLine(
                "La primera vez, ponlo tú en su iPhone",
                icon = Icons.Default.Lock,
                detail = "Ajustes, Tiempo de uso, Bloquear ajustes de Tiempo de uso. Escríbelo sin que lo vea.",
                badge = Sp.Linea
            )
            StepLine(
                "Que ponga sus límites",
                icon = Icons.Default.HourglassBottom,
                detail = "En Límites de uso, con Bloquear al final del límite activado.",
                badge = Sp.Linea
            )
            StepLine(
                "Si te pide más tiempo, tú decides",
                icon = Icons.Default.Key,
                detail = "Escribe el código en su iPhone. Si se lo dictas, cámbialo la próxima vez que os veáis.",
                badge = Sp.Linea
            )
        }
        Spacer(Modifier.height(Space.l))
        OutlineButton("Cambiar el código", { proposed = IosCodes.newCode() })
    }

    proposed?.let { next ->
        AlertDialog(
            onDismissRequest = { proposed = null },
            title = { Text("Código nuevo: ${next.toCharArray().joinToString(" ")}") },
            text = {
                Text(
                    "Ponlo en su iPhone: Ajustes, Tiempo de uso, Cambiar código de Tiempo de uso. Te pedirá el de ahora (${code}). Hasta que lo cambies, sigue valiendo el de ahora.",
                    color = Sp.TextoSuave
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.friendDao().setScreenTimeCode(friend.id, next)
                        code = next
                        proposed = null
                    }
                }) { Text("Ya lo he cambiado") }
            },
            dismissButton = { TextButton(onClick = { proposed = null }) { Text("Cancelar", color = Sp.TextoSuave) } }
        )
    }
}
