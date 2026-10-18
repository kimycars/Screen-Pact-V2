package com.screenpact.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.screenpact.app.data.FriendCodes
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.ui.components.CodeInput
import com.screenpact.app.ui.theme.Sp
import kotlinx.coroutines.launch
import com.screenpact.app.ui.theme.Space

/**
 * Pide la llave de 6 cifras de un amigo antes de una acción que afloja el control
 * (quitar una app, subir un límite, romper un pacto...). Llama a [onVerified] solo si la llave
 * es válida para alguno de los amigos con poder de desbloqueo.
 */
@Composable
fun FriendCodeDialog(
    reason: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hace falta una llave") },
        text = {
            Column {
                Text(reason, color = Sp.TextoSuave)
                Spacer(Modifier.height(Space.m))
                CodeInput(code = code, onCodeChange = { code = it; error = null })
                error?.let {
                    Spacer(Modifier.height(Space.xs))
                    Text(it, color = Sp.Aviso, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        if (FriendCodes.match(db, code) != null) onVerified()
                        else {
                            error = "Esa llave no vale. Puede que haya caducado: pide la que sale ahora."
                            code = ""
                        }
                    }
                },
                enabled = code.length == 6
            ) { Text("Usar llave") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Sp.TextoSuave) } }
    )
}
