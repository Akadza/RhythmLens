package com.rimuru.android.rhythmlens.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rimuru.android.rhythmlens.ui.screens.MainScreen
import com.rimuru.android.rhythmlens.ui.screens.ecg.EcgDetailScreen
import com.rimuru.android.rhythmlens.ui.screens.history.HistoryScreen
import com.rimuru.android.rhythmlens.ui.screens.home.HomeScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = BottomNavDestination.Home
    ) {

        composable<BottomNavDestination.Home> { MainScreen(navController) }
        composable<BottomNavDestination.History> { MainScreen(navController) }
        composable<BottomNavDestination.Patients> { MainScreen(navController) }
        composable<BottomNavDestination.Profile> { MainScreen(navController) }

        // Детальные полноэкранные экраны
        composable<EcgDetailDestination> { backStackEntry ->
            val destination: EcgDetailDestination = backStackEntry.toRoute()
            EcgDetailScreen(
                ecgId = destination.ecgId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ComparisonDestination> { backStackEntry ->
            val destination: ComparisonDestination = backStackEntry.toRoute()
            // ComparisonScreen(destination.ecgIds) — позже
        }

        composable<ScanDestination> {
            // ScanScreen() — позже
        }

        composable<SyntheticImageDestination> { backStackEntry ->
            val destination: SyntheticImageDestination = backStackEntry.toRoute()
            // SyntheticImageScreen(destination.ecgId) — позже
        }
    }
}