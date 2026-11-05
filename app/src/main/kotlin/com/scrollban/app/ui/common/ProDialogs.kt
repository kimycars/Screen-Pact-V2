package com.scrollban.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.scrollban.app.data.Pro
import com.scrollban.app.ui.components.ChipButton
import com.scrollban.app.ui.components.SbTextField
import com.scrollban.app.ui.theme.Sb
import com.scrollban.app.ui.theme.Space

/** Pegar una licencia de Pro a mano (si el enlace de la compra no abrió la app). */
@Composable
fun LicenseDialog(onDismiss: () -> Unit, onActivated: () -> Unit) {
    val context = LocalContext.current
    var token by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Activar Pro") },
        text = {
            Column {
                Text("Pega el código de licencia que te dimos al comprar.", color = Sb.TextoSuave)
                Spacer(Modifier.height(Space.m))
                SbTextField(value = token, onValueChange = { token = it; error = null }, label = "Código de licencia")
                error?.let {
                    Spacer(Modifier.height(Space.xs))
                    Text(it, color = Sb.Aviso, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (Pro.activate(context, token)) onActivated()
                else error = "Ese código no es válido. Cópialo entero, sin espacios al principio ni al final."
            }, enabled = token.isNotBlank()) { Text("Activar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Sb.TextoSuave) } }
    )
}

/**
 * Elegir un horario de cierre (de [start] a [end], en minutos desde medianoche).
 * Dos pasos: primero la hora de inicio y luego la de fin, con el teclado de horas de Material.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(
    appName: String,
    initialStart: Int,
    initialEnd: Int,
    onDismiss: () -> Unit,
    onConfirm: (start: Int, end: Int) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    var step by remember { mutableIntStateOf(0) }
    var start by remember { mutableIntStateOf(initialStart) }
    val startState = rememberTimePickerState(initialStart / 60, initialStart % 60, is24Hour = true)
    val endState = rememberTimePickerState(initialEnd / 60, initialEnd % 60, is24Hour = true)
    val colors = TimePickerDefaults.colors(
        timeSelectorSelectedContainerColor = Sb.Llave,
        timeSelectorSelectedContentColor = Sb.Tinta,
        timeSelectorUnselectedContainerColor = Sb.Superficie,
        timeSelectorUnselectedContentColor = Sb.Niebla
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 0) "$appName cerrada desde…" else "…hasta") },
        text = {
            Column {
                Text(
                    "Durante ese horario $appName se bloquea aunque te quede tiempo. Ampliarlo es libre; acortarlo o quitarlo pide la llave de un amigo.",
                    color = Sb.TextoSuave
                )
                Spacer(Modifier.height(Space.m))
                if (step == 0) TimeInput(state = startState, colors = colors) else TimeInput(state = endState, colors = colors)
                if (onRemove != null && step == 0) {
                    Row { ChipButton("Quitar horario", onRemove) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (step == 0) {
                    start = startState.hour * 60 + startState.minute
                    step = 1
                } else {
                    onConfirm(start, endState.hour * 60 + endState.minute)
                }
            }) { Text(if (step == 0) "Siguiente" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Sb.TextoSuave) } }
    )
}
