package com.rimuru.android.rhythmlens.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.rimuru.android.rhythmlens.ui.app.features.history.HistoryRoute
import com.rimuru.android.rhythmlens.ui.app.features.home.HomeRoute
import com.rimuru.android.rhythmlens.ui.app.features.patients.PatientsRoute
import com.rimuru.android.rhythmlens.ui.app.features.profile.ProfileRoute

fun NavGraphBuilder.bottomNavGraph(
    rootNavController: NavHostController
) {
    composable<BottomNavDestination.Home> {
        HomeRoute(
            onNavigateToEcgDetail = { ecgId ->
                rootNavController.navigate(
                    EcgDetailDestination(ecgId)
                )
            }
        )
    }

    composable<BottomNavDestination.History> {
        HistoryRoute(
            onNavigateToEcgDetail = { ecgId ->
                rootNavController.navigate(
                    EcgDetailDestination(ecgId)
                )
            }
        )
    }

    composable<BottomNavDestination.Patients> {
        PatientsRoute()
    }

    composable<BottomNavDestination.Profile> {
        ProfileRoute()
    }
}
