package com.screenpact.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.screenpact.app.ui.apps.AppLimitsScreen
import com.screenpact.app.ui.friends.AddFriendScreen
import com.screenpact.app.ui.friends.FriendsScreen
import com.screenpact.app.ui.friends.GenerateCodeScreen
import com.screenpact.app.ui.home.HomeScreen
import com.screenpact.app.ui.home.OnboardingScreen

object Routes {
    const val ONBOARD = "onboard"
    const val HOME = "home"
    const val FRIENDS = "friends"
    const val ADD_FRIEND = "add_friend"
    const val GENERATE_CODE = "generate/{friendId}"
    const val APP_LIMITS = "app_limits"

    fun generate(friendId: Long) = "generate/$friendId"
}

@Composable
fun MainNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.ONBOARD) {
        composable(Routes.ONBOARD) { OnboardingScreen(onContinue = { nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARD) { inclusive = true } } }) }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenFriends = { nav.navigate(Routes.FRIENDS) },
                onOpenLimits = { nav.navigate(Routes.APP_LIMITS) }
            )
        }
        composable(Routes.FRIENDS) {
            FriendsScreen(
                onAdd = { nav.navigate(Routes.ADD_FRIEND) },
                onOpenFriend = { id -> nav.navigate(Routes.generate(id)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.ADD_FRIEND) {
            AddFriendScreen(onDone = { nav.popBackStack() })
        }
        composable(Routes.GENERATE_CODE) { backStack ->
            val id = backStack.arguments?.getString("friendId")?.toLongOrNull() ?: -1L
            GenerateCodeScreen(friendId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.APP_LIMITS) {
            AppLimitsScreen(onBack = { nav.popBackStack() })
        }
    }
}
