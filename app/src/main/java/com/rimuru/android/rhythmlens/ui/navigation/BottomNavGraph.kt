package com.rimuru.android.rhythmlens.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.rimuru.android.rhythmlens.ui.screens.history.HistoryScreen
import com.rimuru.android.rhythmlens.ui.screens.home.HomeScreen

fun NavGraphBuilder.bottomNavGraph(navController: NavHostController) {
    composable<BottomNavDestination.Home> {
        HomeScreen(
            onScanClick = { navController.navigate(ScanDestination) },
            onGalleryClick = { /* TODO: открыть галерею */ },
            onImportClick = { /* TODO: открыть импорт файла */ }
        )
    }

    composable<BottomNavDestination.History> {
        HistoryScreen(
            onEcgClick = { ecgId ->
                navController.navigate(EcgDetailDestination(ecgId))
            },
            onCompareClick = { selectedIds ->
                navController.navigate(ComparisonDestination(selectedIds))
            }
        )
    }

    composable<BottomNavDestination.Patients> {
        // PatientsScreen() — позже
    }

    composable<BottomNavDestination.Profile> {
        // ProfileScreen() — позже
    }
}