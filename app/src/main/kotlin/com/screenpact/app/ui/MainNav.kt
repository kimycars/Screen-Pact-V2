package com.screenpact.app.ui

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.screenpact.app.ui.apps.AppLimitsScreen
import com.screenpact.app.ui.friends.AddFriendScreen
import com.screenpact.app.ui.friends.FriendsScreen
import com.screenpact.app.ui.friends.GenerateCodeScreen
import com.screenpact.app.ui.home.HomeScreen
import com.screenpact.app.ui.home.OnboardingScreen
import com.screenpact.app.ui.pro.ProScreen
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.ui.settings.SettingsScreen
import com.screenpact.app.ui.history.HistoryScreen

object Routes {
    const val ONBOARD = "onboard"
    const val HOME = "home"
    const val LIMITS = "limits"
    const val FRIENDS = "friends"
    const val PAIR = "pair"
    const val GENERATE_CODE = "generate/{friendId}"
    const val PRO = "pro"
    const val SETTINGS = "settings"
    const val HISTORY = "history"

    fun generate(friendId: Long) = "generate/$friendId"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Hoy", Icons.Default.Schedule),
    Tab(Routes.LIMITS, "Límites", Icons.AutoMirrored.Filled.List),
    Tab(Routes.FRIENDS, "Amigos", Icons.Default.People),
)

@Composable
fun MainNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showTabs = tabs.any { it.route == route }

    Column(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = Routes.ONBOARD, modifier = Modifier.weight(1f)) {
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
                    onOpenPro = { nav.navigate(Routes.PRO) }
                )
            }
            composable(Routes.LIMITS) {
                AppLimitsScreen(onOpenPro = { nav.navigate(Routes.PRO) })
            }
            composable(Routes.FRIENDS) {
                FriendsScreen(
                    onAdd = { nav.navigate(Routes.PAIR) },
                    onOpenFriend = { id -> nav.navigate(Routes.generate(id)) }
                )
            }
            composable(Routes.PAIR) {
                AddFriendScreen(onDone = { nav.popBackStack() })
            }
            composable(Routes.GENERATE_CODE) { backStack ->
                val id = backStack.arguments?.getString("friendId")?.toLongOrNull() ?: -1L
                GenerateCodeScreen(friendId = id, onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() }, onOpenPro = { nav.navigate(Routes.PRO) })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.PRO) {
                ProScreen(onClose = { nav.popBackStack() })
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
    HorizontalDivider(color = Sp.Linea)
    Row(Modifier.fillMaxWidth().height(Sizes.bottomBar)) {
        for (tab in tabs) {
            val selected = tab.route == current
            val color = if (selected) Sp.Llave else Sp.TextoSuave
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
