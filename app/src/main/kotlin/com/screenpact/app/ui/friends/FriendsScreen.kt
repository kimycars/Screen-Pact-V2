package com.screenpact.app.ui.friends

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
                "Cada pacto va en los dos sentidos: tú guardas su llave y él la tuya.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sp.TextoSuave,
                modifier = Modifier.padding(top = Space.xxs, bottom = Space.s)
            )
        }

        if (friends.isEmpty()) {
            item {
                SpCard {
                    Text("Aún no tienes pacto", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Space.xxs))
                    Text(
                        "Queda con alguien que también tenga ScreenPact. Con los dos móviles delante se hace en un minuto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sp.TextoSuave
                    )
                }
            }
        } else {
            items(friends, key = { it.id }) { f ->
                val waitMs = FriendCodes.cooldownLeftMs(f, friends)
                SpCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(f.name)
                        Spacer(Modifier.width(Space.m))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (waitMs > 0) "Podrá desbloquearte en ${(waitMs + 3_599_999) / 3_600_000} h"
                                else "Tiene tu llave",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sp.TextoSuave
                            )
                        }
                        ChipButton("Darle llave", { onOpenFriend(f.id) })
                        IconAction(Icons.Default.Close, "Romper el pacto con ${f.name}", { toDelete = f })
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.xs))
            PrimaryButton(if (friends.isEmpty()) "Sellar un pacto" else "Sellar otro pacto", onAdd)
            Spacer(Modifier.height(Space.xs))
            OutlineButton("Invitar a un amigo", { Invite.share(context) })
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
