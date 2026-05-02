package com.rimuru.android.rhythmlens.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.rimuru.android.rhythmlens.ui.navigation.BottomNavDestination
import com.rimuru.android.rhythmlens.ui.navigation.BottomNavigationBar
import com.rimuru.android.rhythmlens.ui.navigation.EcgDetailDestination
import com.rimuru.android.rhythmlens.ui.screens.history.HistoryScreen
import com.rimuru.android.rhythmlens.ui.screens.home.HomeScreen

@Composable
fun MainScreen(navController: NavHostController) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    val currentBottomNav = when (currentDestination?.route) {
        BottomNavDestination.Home::class.qualifiedName -> BottomNavDestination.Home
        BottomNavDestination.History::class.qualifiedName -> BottomNavDestination.History
        BottomNavDestination.Patients::class.qualifiedName -> BottomNavDestination.Patients
        BottomNavDestination.Profile::class.qualifiedName -> BottomNavDestination.Profile
        else -> BottomNavDestination.Home
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentDestination = currentBottomNav,
                onNavigate = { destination ->
                    navController.navigate(destination) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        when (currentBottomNav) {
            is BottomNavDestination.Home -> HomeScreen(
                modifier = Modifier.padding(innerPadding),
                onScanClick = { /* TODO */ },
                onGalleryClick = { /* TODO */ },
                onImportClick = { /* TODO */ }
            )
            is BottomNavDestination.History -> HistoryScreen(
                modifier = Modifier.padding(innerPadding),
                onEcgClick = { ecgId -> navController.navigate(EcgDetailDestination(ecgId)) },
                onCompareClick = { /* TODO */ }
            )
            is BottomNavDestination.Patients -> { /* TODO */ }
            is BottomNavDestination.Profile -> { /* TODO */ }
        }
    }
}