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
import com.screenpact.app.data.History
import com.screenpact.app.data.TimeWon
import com.screenpact.app.data.cloud.Together
import com.screenpact.app.data.cloud.hasData
import com.screenpact.app.data.cloud.weekUsedMs
import com.screenpact.app.data.Challenges
import com.screenpact.app.data.db.Challenge
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.Friend
import com.screenpact.app.data.db.PactGroup
import com.screenpact.app.data.cloud.Cloud
import androidx.compose.runtime.mutableIntStateOf
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
fun FriendsScreen(onAdd: () -> Unit, onOpenFriend: (Long) -> Unit, onOpenMember: (String, Together.Member) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val friends by db.friendDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    // Borrar amigos pide llave: si no, borrarlos todos dejaba los límites sin protección.
    var toDelete by remember { mutableStateOf<Friend?>(null) }
    val limits by db.appLimitDao().observeAll().collectAsState(initial = emptyList())
    val challenge by db.challengeDao().observeLatest().collectAsState(initial = null)
    var progress by remember { mutableStateOf<Challenges.Progress?>(null) }
    var creating by remember { mutableStateOf(false) }
    var quitting by remember { mutableStateOf<Challenge?>(null) }
    // Juntos en el servidor (ranking, tiempo del grupo y retos de los amigos). Opcional.
    var togetherOn by remember { mutableStateOf(Together.isEnabled(context)) }
    var enabling by remember { mutableStateOf(false) }
    var enableFailed by remember { mutableStateOf(false) }
    var board by remember { mutableStateOf<Together.Board?>(null) }
    // Grupos de 3 o más.
    val groups by db.pactGroupDao().observeAll().collectAsState(initial = emptyList())
    var refresh by remember { mutableIntStateOf(0) }
    var creatingGroup by remember { mutableStateOf(false) }
    var invitingTo by remember { mutableStateOf<PactGroup?>(null) }
    var leavingGroup by remember { mutableStateOf<PactGroup?>(null) }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var joiningId by remember { mutableStateOf<String?>(null) }
    var joinError by remember { mutableStateOf<Pair<String, String>?>(null) }
    var declined by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(togetherOn, friends.size, groups.size, challenge?.id, refresh) {
        if (!togetherOn || (friends.isEmpty() && groups.isEmpty())) return@LaunchedEffect
        var force = true
        while (true) {
            runCatching {
                Together.sync(context, force)
                Together.board(context)
            }.onSuccess { board = it }
            force = false
            delay(60_000)
        }
    }
    LaunchedEffect(challenge) {
        val c = challenge
        if (c == null) { progress = null; return@LaunchedEffect }
        while (true) {
            progress = Challenges.progress(db, c)
            delay(30_000)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.s)
    ) {
        item { ScreenTitle("Juntos") }
        item {
            Text(
                "Cada pacto va en los dos sentidos: tú guardas su llave y tu amigo guarda la tuya.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sp.TextoSuave,
                modifier = Modifier.padding(top = Space.xxs, bottom = Space.s)
            )
        }

        val b = board
        // Invitaciones a grupos: lo primero, porque esperan una respuesta.
        b?.invites?.filter { inv -> inv.groupId !in declined && groups.none { it.id == inv.groupId } }?.forEach { inv ->
            item(key = "inv-${inv.groupId}") {
                GroupInviteCard(
                    inv,
                    busy = joiningId == inv.groupId,
                    error = joinError?.takeIf { it.first == inv.groupId }?.second,
                    onJoin = {
                        joiningId = inv.groupId
                        joinError = null
                        scope.launch {
                            runCatching { Together.accept(context, inv) }.onFailure {
                                joinError = inv.groupId to (if ((it as? Cloud.CloudException)?.code == 400) "El grupo ya está lleno." else "No se ha podido conectar. Vuelve a probar.")
                            }
                            joiningId = null
                            refresh++
                        }
                    },
                    onDecline = {
                        declined = declined + inv.groupId
                        scope.launch { runCatching { Together.decline(context, inv) } }
                    }
                )
            }
        }
        if (friends.any { !it.ios }) {
            if (!togetherOn) {
                item {
                    TogetherOffCard(busy = enabling, error = enableFailed, onEnable = {
                        enabling = true
                        scope.launch {
                            val ok = runCatching { Together.enable(context) }.isSuccess
                            enabling = false
                            enableFailed = !ok
                            togetherOn = ok
                            if (!ok) runCatching { Together.disable(context) }
                        }
                    })
                }
            } else if (b != null) {
                item {
                    // Los amigos con iPhone no tienen la app, así que no salen en el ranking.
                    val others = friends.filter { !it.ios }.map { f -> f.name to b.friends[f.id] } +
                        groupOnlyMembers(b).map { m -> m.name to b.people[m.uid] }
                    val rows = listOf(RankRow("Tú", true, b.me.weekUsedMs().takeIf { b.me.hasData }, TimeWon.summaryOf(b.me.records, b.me.baselines).wonMs)) +
                        others.map { (name, person) ->
                            RankRow(
                                name, false,
                                person?.takeIf { it.hasData }?.weekUsedMs(),
                                person?.let { TimeWon.summaryOf(it.records, it.baselines).wonMs } ?: 0L
                            )
                        }
                    val people = listOf(b.me) + b.people.values
                    val groupWon = people.sumOf { TimeWon.summaryOf(it.records, it.baselines, window = 30).wonMs }
                    RankingCard(rows, groupWon)
                }
            }
        }

        // El reto va arriba: es lo que cambia cada día. Solo se mide en apps con límite.
        val p = progress
        if (p != null) {
            val partners = if (b == null) emptyList() else namedPeople(friends, b).filter { (_, person) -> person.challenges.any { it.id == p.challenge.id } }.map { it.first }
            item { ChallengeCard(p, onNew = { creating = true }, onQuit = { quitting = p.challenge }, partners = partners) }
        } else if (limits.isNotEmpty()) {
            item { NoChallengeCard(onStart = { creating = true }) }
        }

        // Los retos de tus amigos que no son el tuyo.
        if (b != null) {
            namedPeople(friends, b).forEachIndexed { i, (name, person) ->
                person.challenges.filter { it.id != p?.challenge?.id }.forEach { c ->
                    val fp = Challenges.progressOf(c, person.records)
                    if (fp.finished && fp.days.last().first < History.daysAgo(3)) return@forEach
                    item(key = "fc-$i-${c.id}") {
                        FriendChallengeCard(
                            name = name,
                            progress = fp,
                            canJoin = limits.any { it.packageName == c.packageName && it.enabled },
                            onJoin = { scope.launch { Challenges.join(db, c) } }
                        )
                    }
                }
            }
        }

        // Grupos de 3 o más: hacen falta el ranking (para invitar) y al menos un pacto.
        if (togetherOn && b != null && friends.isNotEmpty()) {
            groups.forEach { g ->
                item(key = "g-${g.id}") {
                    GroupCard(
                        group = g,
                        members = b.groups[g.id].orEmpty(),
                        waitMs = FriendCodes.cooldownLeftMs(g),
                        canInvite = true,
                        onGiveKey = { m -> m.friendId?.let(onOpenFriend) ?: onOpenMember(g.id, m) },
                        onInvite = { actionError = null; invitingTo = g },
                        onLeave = { leavingGroup = g }
                    )
                }
            }
            item(key = "g-new") {
                if (groups.isEmpty()) NoGroupCard { actionError = null; creatingGroup = true }
                else QuietButton("Crear otro grupo", { actionError = null; creatingGroup = true }, modifier = Modifier.fillMaxWidth())
            }
        } else if (groups.isNotEmpty()) {
            // Sin ranking los grupos siguen dando llaves, pero no se ve quién está.
            items(groups, key = { "g-${it.id}" }) { g ->
                GroupCard(g, emptyList(), FriendCodes.cooldownLeftMs(g), canInvite = false, onGiveKey = {}, onInvite = {}, onLeave = { leavingGroup = g })
            }
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

    if (creating && limits.isNotEmpty()) {
        StartChallengeDialog(
            limits = limits,
            onDismiss = { creating = false },
            onStart = { app, minutes, days ->
                creating = false
                scope.launch { Challenges.start(db, app.packageName, app.appName, minutes, days) }
            }
        )
    }

    quitting?.let { c ->
        QuitChallengeDialog(c, onDismiss = { quitting = null }, onQuit = {
            quitting = null
            scope.launch { db.challengeDao().delete(c.id) }
        })
    }

    if (creatingGroup) {
        CreateGroupDialog(busy = busy, error = actionError, onDismiss = { creatingGroup = false }, onCreate = { name ->
            busy = true
            actionError = null
            scope.launch {
                runCatching { Together.createGroup(context, name) }
                    .onSuccess { creatingGroup = false; refresh++ }
                    .onFailure { actionError = "No se ha podido conectar. Vuelve a probar." }
                busy = false
            }
        })
    }

    invitingTo?.let { g ->
        val bb = board ?: return@let
        val (candidates, missing) = inviteCandidates(friends, bb, bb.groups[g.id].orEmpty())
        InviteToGroupDialog(g.name, candidates, missing, busy, actionError, onDismiss = { invitingTo = null }, onInvite = { f ->
            busy = true
            actionError = null
            scope.launch {
                runCatching { Together.invite(context, g, f, bb.friendUids.getValue(f.id)) }
                    .onSuccess { invitingTo = null }
                    .onFailure {
                        actionError = if ((it as? Cloud.CloudException)?.code == 400) "El grupo ya está lleno." else "No se ha podido conectar. Vuelve a probar."
                    }
                busy = false
            }
        })
    }

    // Salir de un grupo pide llave, como romper un pacto: te deja con menos gente que pueda dártela.
    leavingGroup?.let { g ->
        FriendCodeDialog(
            reason = "Para salir de ${g.name} necesitas una llave (puede ser de alguien del grupo).",
            onDismiss = { leavingGroup = null },
            onVerified = {
                leavingGroup = null
                scope.launch { Together.leaveGroup(context, g); refresh++ }
            }
        )
    }

    toDelete?.let { f ->
        FriendCodeDialog(
            reason = "Para romper el pacto con ${f.name} necesitas la llave de un amigo (puede ser la suya).",
            onDismiss = { toDelete = null },
            onVerified = {
                toDelete = null
                scope.launch {
                    Together.leave(context, f)
                    db.friendDao().delete(f)
                }
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
                Text(
                    if (friend.ios) "Con iPhone. Pacto desde el ${formatDay(friend.addedAt)}" else "Pacto desde el ${formatDay(friend.addedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sp.TextoSuave
                )
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
        PrimaryButton(if (friend.ios) "Ver su código de Tiempo de uso" else "Darle su llave", onGiveKey)
    }
}

/** Gente de tus grupos con la que no tienes pacto (sin repetir a nadie). */
private fun groupOnlyMembers(b: Together.Board): List<Together.Member> =
    b.groups.values.flatten().filter { it.friendId == null }.distinctBy { it.uid }

/** Cada persona con su nombre: tus amigos y la gente de tus grupos. */
private fun namedPeople(friends: List<Friend>, b: Together.Board): List<Pair<String, Together.Person>> =
    friends.mapNotNull { f -> b.friends[f.id]?.let { f.name to it } } +
        groupOnlyMembers(b).mapNotNull { m -> b.people[m.uid]?.let { m.name to it } }
