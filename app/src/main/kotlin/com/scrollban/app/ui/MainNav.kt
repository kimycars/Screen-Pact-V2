package com.scrollban.app.ui

import android.net.Uri

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import com.scrollban.app.ui.theme.Motion
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scrollban.app.ui.apps.AppLimitsScreen
import com.scrollban.app.ui.friends.AddFriendScreen
import com.scrollban.app.ui.friends.AcceptPactScreen
import com.scrollban.app.ui.friends.IncomingPact
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.scrollban.app.ui.friends.FriendsScreen
import com.scrollban.app.ui.friends.GenerateCodeScreen
import com.scrollban.app.ui.friends.KeyFor
import com.scrollban.app.ui.home.HomeScreen
import com.scrollban.app.ui.home.OnboardingScreen
import com.scrollban.app.ui.pro.ProFeature
import com.scrollban.app.ui.pro.ProScreen
import com.scrollban.app.ui.theme.Sb
import com.scrollban.app.ui.theme.Space
import com.scrollban.app.ui.theme.Sizes
import com.scrollban.app.ui.settings.SettingsScreen
import com.scrollban.app.ui.history.HistoryScreen

object Routes {
    const val ONBOARD = "onboard"
    const val HOME = "home"
    const val LIMITS = "limits"
    const val FRIENDS = "friends"
    const val PAIR = "pair"
    const val GENERATE_CODE = "generate/{friendId}"
    const val GROUP_KEY = "groupkey/{groupId}/{installId}?name={name}"
    const val PRO = "pro/{feature}"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val ACCEPT = "accept"

    fun generate(friendId: Long) = "generate/$friendId"
    fun groupKey(groupId: String, installId: String, name: String) = "groupkey/$groupId/$installId?name=${Uri.encode(name)}"
    fun pro(feature: ProFeature) = "pro/${feature.id}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Hoy", Icons.Default.Schedule),
    Tab(Routes.FRIENDS, "Juntos", Icons.Default.People),
    Tab(Routes.LIMITS, "Límites", Icons.AutoMirrored.Filled.List),
)

@Composable
fun MainNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showTabs = tabs.any { it.route == route }

    // Invitación a un pacto que llega por enlace: se abre en cuanto la bienvenida está hecha.
    val incoming by IncomingPact.text.collectAsState()
    LaunchedEffect(incoming, route) {
        if (incoming != null && route != null && route != Routes.ONBOARD && route != Routes.ACCEPT) {
            nav.navigate(Routes.ACCEPT)
        }
    }

    Column(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Routes.ONBOARD,
            modifier = Modifier.weight(1f),
            enterTransition = { fadeIn(tween(Motion.SCREEN_MS)) },
            exitTransition = { fadeOut(tween(Motion.SCREEN_MS)) },
            popEnterTransition = { fadeIn(tween(Motion.SCREEN_MS)) },
            popExitTransition = { fadeOut(tween(Motion.SCREEN_MS)) },
        ) {
            composable(Routes.ONBOARD) {
                OnboardingScreen(
                    onFinished = { pairNow ->
                        nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARD) { inclusive = true } }
                        if (pairNow) nav.navigate(Routes.PAIR)
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onPair = { nav.navigate(Routes.PAIR) },
                    onAddApps = { nav.switchTab(Routes.LIMITS) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenHistory = { nav.navigate(Routes.HISTORY) },
                    onOpenPro = { nav.navigate(Routes.pro(it)) }
                )
            }
            composable(Routes.LIMITS) {
                AppLimitsScreen(onOpenPro = { nav.navigate(Routes.pro(it)) })
            }
            composable(Routes.FRIENDS) {
                FriendsScreen(
                    onAdd = { nav.navigate(Routes.PAIR) },
                    onOpenFriend = { id -> nav.navigate(Routes.generate(id)) },
                    onOpenMember = { g, m -> m.installId?.let { nav.navigate(Routes.groupKey(g, it, m.name)) } }
                )
            }
            composable(Routes.PAIR) {
                AddFriendScreen(
                    onDone = { nav.popBackStack() },
                    onOpenInvite = { IncomingPact.text.value = it }
                )
            }
            composable(Routes.GENERATE_CODE) { backStack ->
                val id = backStack.arguments?.getString("friendId")?.toLongOrNull() ?: -1L
                GenerateCodeScreen(KeyFor.Friend(id), onBack = { nav.popBackStack() })
            }
            composable(Routes.GROUP_KEY) { backStack ->
                val a = backStack.arguments
                GenerateCodeScreen(
                    KeyFor.GroupMember(a?.getString("groupId").orEmpty(), a?.getString("installId").orEmpty(), a?.getString("name").orEmpty()),
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() }, onOpenPro = { nav.navigate(Routes.pro(it)) })
            }
            composable(Routes.ACCEPT) {
                // La invitación se lee una vez al entrar; así un enlace nuevo no cambia la pantalla a medias.
                val text = remember { IncomingPact.text.value.orEmpty() }
                LaunchedEffect(Unit) { IncomingPact.text.value = null }
                AcceptPactScreen(text, onDone = { nav.popBackStack() })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.PRO) { backStack ->
                ProScreen(feature = ProFeature.byId(backStack.arguments?.getString("feature")), onClose = { nav.popBackStack() })
            }
        }
        if (showTabs) BottomTabs(current = route, onSelect = { nav.switchTab(it) })
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomTabs(current: String?, onSelect: (String) -> Unit) {
    HorizontalDivider(color = Sb.Linea)
    Row(Modifier.fillMaxWidth().height(Sizes.bottomBar)) {
        for (tab in tabs) {
            val selected = tab.route == current
            val color = if (selected) Sb.Llave else Sb.TextoSuave
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab.route) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(Sizes.iconL))
                Text(tab.label, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(top = Space.xxs))
            }
        }
    }
}
