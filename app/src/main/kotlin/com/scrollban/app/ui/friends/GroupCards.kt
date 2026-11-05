package com.scrollban.app.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.scrollban.app.data.FriendCodes
import com.scrollban.app.data.cloud.Together
import com.scrollban.app.data.db.Friend
import com.scrollban.app.data.db.PactGroup
import com.scrollban.app.ui.components.Avatar
import com.scrollban.app.ui.components.ChipButton
import com.scrollban.app.ui.components.ChoiceChip
import com.scrollban.app.ui.components.OutlineButton
import com.scrollban.app.ui.components.PrimaryButton
import com.scrollban.app.ui.components.QuietButton
import com.scrollban.app.ui.components.SbCard
import com.scrollban.app.ui.components.SbTextField
import com.scrollban.app.ui.components.StepLine
import com.scrollban.app.ui.theme.Sizes
import com.scrollban.app.ui.theme.Sb
import com.scrollban.app.ui.theme.Space

/** Tope de personas por grupo (el servidor no deja más). */
const val GROUP_MAX_PEOPLE = 8

/** Sin grupos: qué es un grupo y el botón para crear uno. */
@Composable
fun NoGroupCard(onCreate: () -> Unit) {
    SbCard {
        StepLine(
            "Haced un grupo",
            icon = Icons.Default.Groups,
            detail = "Con tres o más, cualquiera del grupo puede darte la llave y todos veis el ranking.",
            badge = Sb.Linea
        )
        Spacer(Modifier.height(Space.s))
        OutlineButton("Crear un grupo", onCreate)
    }
}

/** Un amigo te invita a su grupo. [error]: el último intento falló. */
@Composable
fun GroupInviteCard(invite: Together.GroupInvite, busy: Boolean, error: String?, onJoin: () -> Unit, onDecline: () -> Unit) {
    SbCard {
        Text("${invite.from.name} te invita a su grupo", style = MaterialTheme.typography.labelMedium, color = Sb.TextoSuave)
        Spacer(Modifier.height(Space.xxs))
        Text(invite.name, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Space.xxs))
        Text(
            "Cualquiera del grupo podrá darte la llave, a partir de 24 h después de entrar, y verá tus minutos en las apps con límite.",
            style = MaterialTheme.typography.bodySmall,
            color = Sb.TextoSuave
        )
        error?.let {
            Spacer(Modifier.height(Space.xs))
            Text(it, style = MaterialTheme.typography.bodySmall, color = Sb.Llave)
        }
        Spacer(Modifier.height(Space.m))
        PrimaryButton(if (busy) "Entrando…" else "Unirme al grupo", onJoin, enabled = !busy)
        Spacer(Modifier.height(Space.xs))
        QuietButton("Ahora no", onDecline)
    }
}

/**
 * Un grupo: quién está, la llave para cada uno y cómo meter a otro amigo. [waitMs]: lo que falta
 * para que sus llaves te valgan (24 h desde que entraste).
 */
@Composable
fun GroupCard(
    group: PactGroup,
    members: List<Together.Member>,
    waitMs: Long,
    canInvite: Boolean,
    onGiveKey: (Together.Member) -> Unit,
    onInvite: () -> Unit,
    onLeave: () -> Unit,
) {
    SbCard {
        Text("Grupo", style = MaterialTheme.typography.labelMedium, color = Sb.TextoSuave)
        Text(group.name, style = MaterialTheme.typography.titleLarge)
        Text(
            when {
                waitMs > 0 -> "Sus llaves te valdrán en ${(waitMs + 3_599_999) / 3_600_000} h"
                group.keysGiven == 1 -> "${members.size + 1} personas. Te han dado 1 llave."
                group.keysGiven > 1 -> "${members.size + 1} personas. Te han dado ${group.keysGiven} llaves."
                else -> "${members.size + 1} personas"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (waitMs > 0) Sb.Llave else Sb.TextoSuave
        )
        Spacer(Modifier.height(Space.m))
        if (members.isEmpty()) {
            Text("Aún no hay nadie más. Añade a un amigo con el que tengas pacto.", style = MaterialTheme.typography.bodyMedium, color = Sb.TextoSuave)
            Spacer(Modifier.height(Space.s))
        }
        members.forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(m.name, size = Sizes.avatar)
                Spacer(Modifier.width(Space.s))
                Text(m.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                // Con pacto vale la llave del pacto. Si no, hace falta su id cifrado: aparece en cuanto su móvil sube algo.
                if (m.friendId != null || m.installId != null) ChipButton("Darle su llave", { onGiveKey(m) })
            }
            Spacer(Modifier.height(Space.s))
        }
        if (canInvite && members.size + 1 < GROUP_MAX_PEOPLE) {
            OutlineButton("Añadir a un amigo", onInvite)
            Spacer(Modifier.height(Space.xs))
        }
        QuietButton("Salir del grupo", onLeave)
    }
}

/** Nombre para un grupo nuevo. [error]: el último intento falló. */
@Composable
fun CreateGroupDialog(busy: Boolean, error: String?, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo grupo") },
        text = {
            Column {
                Text(
                    "Después añades a tus amigos con pacto. Quien entre podrá darte la llave a las 24 h.",
                    color = Sb.TextoSuave
                )
                Spacer(Modifier.height(Space.m))
                SbTextField(name, { name = it.take(Together.MAX_GROUP_NAME) }, "Nombre del grupo")
                error?.let {
                    Spacer(Modifier.height(Space.xs))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Sb.Llave)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank() && !busy) { Text(if (busy) "Creando…" else "Crear grupo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Sb.TextoSuave) } }
    )
}

/**
 * Elegir a quién invitar al grupo [groupName]. Solo salen amigos cuyo pacto ya da llaves y que
 * tienen el ranking activado ([candidates]); [missing] son los que faltan por eso.
 */
@Composable
fun InviteToGroupDialog(
    groupName: String,
    candidates: List<Friend>,
    missing: List<Friend>,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onInvite: (Friend) -> Unit,
) {
    var chosen by remember { mutableStateOf(candidates.firstOrNull()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir a $groupName") },
        text = {
            Column {
                if (candidates.isEmpty()) {
                    Text("Ahora mismo no puedes añadir a nadie.", color = Sb.TextoSuave)
                } else {
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        candidates.forEach { f -> ChoiceChip(f.name, chosen?.id == f.id, { chosen = f }) }
                    }
                    Spacer(Modifier.height(Space.s))
                    Text("Le llegará la invitación en su pestaña Juntos.", style = MaterialTheme.typography.bodySmall, color = Sb.TextoSuave)
                }
                if (missing.isNotEmpty()) {
                    Spacer(Modifier.height(Space.s))
                    Text(
                        "${missing.joinToString(", ") { it.name }}: aún no, porque su pacto tiene que dar llaves y tiene que haber activado el ranking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sb.TextoSuave
                    )
                }
                error?.let {
                    Spacer(Modifier.height(Space.xs))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Sb.Llave)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { chosen?.let(onInvite) }, enabled = chosen != null && !busy) { Text(if (busy) "Invitando…" else "Invitar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Sb.TextoSuave) } }
    )
}

/** Amigos que se pueden invitar a un grupo y los que aún no (pacto en espera o sin ranking). */
fun inviteCandidates(friends: List<Friend>, board: Together.Board, members: List<Together.Member>): Pair<List<Friend>, List<Friend>> {
    val inGroup = members.map { it.uid }.toSet()
    // Los amigos con iPhone no tienen la app: no pueden estar en un grupo.
    val notIn = friends.filter { !it.ios && (board.friendUids[it.id] !in inGroup || board.friendUids[it.id] == null) }
    return notIn.partition { FriendCodes.canUnlock(it, friends) && board.friendUids[it.id] != null }
}
