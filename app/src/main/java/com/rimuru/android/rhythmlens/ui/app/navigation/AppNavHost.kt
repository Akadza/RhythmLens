package com.rimuru.android.rhythmlens.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.screens.MainScreen
import com.rimuru.android.rhythmlens.ui.screens.ecg.EcgDetailScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainDestination
    ) {
        composable<MainDestination> {
            MainScreen(rootNavController = navController)
        }

        composable<EcgDetailDestination> { backStackEntry ->
            val destination = backStackEntry.toRoute<EcgDetailDestination>()

            EcgDetailScreen(
                ecgId = destination.ecgId,
                onBackClick = { navController.popBackStack() },
                onCompareClick = {
                    navController.navigate(
                        ComparisonDestination(baseEcgId = destination.ecgId)
                    )
                },
                onSyntheticClick = {
                    navController.navigate(
                        SyntheticImageDestination(ecgId = destination.ecgId)
                    )
                },
                onExportClick = {
                    navController.navigate(
                        ExportDestination(ecgId = destination.ecgId)
                    )
                }
            )
        }

        composable<ComparisonDestination> {
            RoutePlaceholderScreen(
                title = stringResource(R.string.screen_comparison)
            )
        }

        composable<ScanDestination> {
            RoutePlaceholderScreen(
                title = stringResource(R.string.screen_scan)
            )
        }

        composable<SyntheticImageDestination> {
            RoutePlaceholderScreen(
                title = stringResource(R.string.screen_synthetic_ecg)
            )
        }

        composable<ExportDestination> {
            RoutePlaceholderScreen(
                title = stringResource(R.string.screen_export)
            )
        }
    }
}

@Composable
private fun RoutePlaceholderScreen(
    title: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}