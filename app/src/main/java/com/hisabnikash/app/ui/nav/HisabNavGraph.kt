package com.hisabnikash.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hisabnikash.app.ui.shell.MainShell

object Routes {
    const val MAIN = "main"
}

@Composable
fun HisabNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainShell(navController)
        }
    }
}
