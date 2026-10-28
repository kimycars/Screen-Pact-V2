package com.screenpact.app.ui.friends

import com.screenpact.app.Brand
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.screenpact.app.data.FriendCodes
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.Friend
import com.screenpact.app.ui.common.FriendCodeDialog
import com.screenpact.app.ui.components.IconAction
import com.screenpact.app.ui.components.Avatar
import com.screenpact.app.ui.components.ChipButton
import com.screenpact.app.ui.components.PrimaryButton
import com.screenpact.app.ui.components.ScreenTitle
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.theme.Sp
import kotlinx.coroutines.launch
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.common.Invite
import com.screenpact.app.ui.components.OutlineButton
import com.screenpact.app.ui.components.QuietButton
import com.screenpact.app.ui.components.StepLine
import com.screenpact.app.ui.components.UsageRing
import com.screenpact.app.ui.common.formatDay
import com.screenpact.app.ui.theme.Sizes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner

@Composable
fun FriendsScreen(onAdd: () -> Unit, onOpenFriend: (Long) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val friends by db.friendDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    // Borrar amigos pide llave: si no, borrarlos todos dejaba los límites sin protección.
    var toDelete by remember { mutableStateOf<Friend?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.s)
    ) {
        item { ScreenTitle("Amigos") }
        item {
            Text(
                "Cada pacto va en los dos sentidos: tú guardas su llave y tu amigo guarda la tuya.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sp.TextoSuave,
                modifier = Modifier.padding(top = Space.xxs, bottom = Space.s)
            )
        }

        if (friends.isEmpty()) {
            item {
                SpCard {
                    Text("Así se sella un pacto", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Space.xs))
                    StepLine("Quedad en persona", icon = Icons.Default.People, detail = "Los dos con ${Brand.NAME} en el móvil.", badge = Sp.Linea)
                    StepLine("Escanead vuestros QR", icon = Icons.Default.QrCodeScanner, detail = "Cada uno el del otro. Tarda un minuto.", badge = Sp.Linea)
                    StepLine("Ya tenéis la llave", icon = Icons.Default.Key, detail = "Si uno se pasa del límite, el otro decide si le da más tiempo.", badge = Sp.Linea)
                }
            }
        } else {
            items(friends, key = { it.id }) { f ->
                FriendCard(
                    friend = f,
                    waitMs = FriendCodes.cooldownLeftMs(f, friends),
                    onGiveKey = { onOpenFriend(f.id) },
                    onBreak = { toDelete = f }
                )
            }
        }

        item {
            Spacer(Modifier.height(Space.xs))
            if (friends.isEmpty()) PrimaryButton("Sellar un pacto", onAdd) else OutlineButton("Sellar otro pacto", onAdd)
            Spacer(Modifier.height(Space.xs))
            QuietButton("Invitar a un amigo", { Invite.share(context) }, modifier = Modifier.fillMaxWidth())
        }
    }

    toDelete?.let { f ->
        FriendCodeDialog(
            reason = "Para romper el pacto con ${f.name} necesitas la llave de un amigo (puede ser la suya).",
            onDismiss = { toDelete = null },
            onVerified = {
                toDelete = null
                scope.launch { db.friendDao().delete(f) }
            }
        )
    }
}

/**
 * Un amigo: avatar (con un anillo de cuenta atrás mientras su llave aún no vale), desde cuándo
 * dura el pacto, cuántas llaves te ha dado y el botón para darle la suya.
 */
@Composable
private fun FriendCard(friend: Friend, waitMs: Long, onGiveKey: () -> Unit, onBreak: () -> Unit) {
    SpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (waitMs > 0) {
                val done = 1f - waitMs.toFloat() / FriendCodes.NEW_FRIEND_COOLDOWN_MS
                UsageRing(done, Sp.Llave, size = Sizes.medal) { Avatar(friend.name, size = Sizes.appIconLarge) }
            } else {
                Avatar(friend.name, size = Sizes.medal)
            }
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(friend.name, style = MaterialTheme.typography.titleMedium)
                Text("Pacto desde el ${formatDay(friend.addedAt)}", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
                Text(
                    when {
                        waitMs > 0 -> "Su llave valdrá en ${(waitMs + 3_599_999) / 3_600_000} h"
                        friend.keysGiven == 0 -> "Aún no te ha dado ninguna llave"
                        friend.keysGiven == 1 -> "Te ha dado 1 llave"
                        else -> "Te ha dado ${friend.keysGiven} llaves"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (waitMs > 0) Sp.Llave else Sp.TextoSuave
                )
            }
            IconAction(Icons.Default.Close, "Romper el pacto con ${friend.name}", onBreak)
        }
        Spacer(Modifier.height(Space.m))
        PrimaryButton("Darle su llave", onGiveKey)
    }
}
