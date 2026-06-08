package com.rimuru.android.rhythmlens.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.app.features.auth.AuthRoute
import com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.EcgDetailRoute
import com.rimuru.android.rhythmlens.ui.app.features.syntheticimage.SyntheticImageRoute
import com.rimuru.android.rhythmlens.ui.screens.MainScreen

@Composable
fun AppNavHost(
    viewModel: AppNavViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

    LaunchedEffect(isAuthenticated) {
        when (isAuthenticated) {
            true -> {
                navController.navigate(MainDestination) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }

            false -> {
                navController.navigate(AuthDestination) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }

            null -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = AuthDestination
    ) {
        composable<AuthDestination> {
            AuthRoute()
        }

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
                }
            )
        }

        composable<ComparisonDestination> {
            RoutePlaceholderScreen(
                title = stringResource(R.string.screen_comparison)
            )
        }

        composable<SyntheticImageDestination> {
            SyntheticImageRoute(
                onNavigateBack = {
                    navController.popBackStack()
                }
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
