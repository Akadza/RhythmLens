package com.rimuru.android.rhythmlens.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rimuru.android.rhythmlens.ui.screens.home.HomeScreen
import com.rimuru.android.rhythmlens.ui.screens.history.HistoryScreen
import com.rimuru.android.rhythmlens.ui.screens.ecg.EcgDetailScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = BottomNavDestination.Home
    ) {
        // Bottom Navigation граф
        bottomNavGraph(navController)

        // Детальные экраны
        composable<EcgDetailDestination> { backStackEntry ->
            val destination: EcgDetailDestination = backStackEntry.toRoute()
            EcgDetailScreen(
                ecgId = destination.ecgId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ComparisonDestination> { /* позже */ }
        composable<ScanDestination> { /* позже */ }
        composable<SyntheticImageDestination> { /* позже */ }
    }
}