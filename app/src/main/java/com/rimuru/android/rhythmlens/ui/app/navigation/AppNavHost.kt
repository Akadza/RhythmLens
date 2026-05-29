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
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.EcgDetailRoute
import com.rimuru.android.rhythmlens.ui.app.features.scan.ScanRoute
import com.rimuru.android.rhythmlens.ui.screens.MainScreen

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

        composable<EcgDetailDestination> {
            EcgDetailRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToComparison = { ecgId ->
                    navController.navigate(
                        ComparisonDestination(baseEcgId = ecgId)
                    )
                },
                onNavigateToSyntheticImage = { ecgId ->
                    navController.navigate(
                        SyntheticImageDestination(ecgId = ecgId)
                    )
                },
                onNavigateToExport = { ecgId ->
                    navController.navigate(
                        ExportDestination(ecgId = ecgId)
                    )
                },
                onOpenDoctorConclusion = {
                    // TODO: открыть экран или диалог заключения врача
                }
            )
        }

        composable<ComparisonDestination> {
            RoutePlaceholderScreen(
                title = stringResource(R.string.screen_comparison)
            )
        }

        composable<ScanDestination> {
            ScanRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEcgDetail = { ecgId ->
                    navController.navigate(
                        EcgDetailDestination(ecgId = ecgId)
                    )
                },
                onOpenCamera = {
                    // TODO: открыть CameraX flow
                },
                onOpenGalleryPicker = {
                    // TODO: открыть системный Photo Picker
                }
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
