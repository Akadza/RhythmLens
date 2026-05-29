package com.rimuru.android.rhythmlens.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.screens.history.HistoryScreen
import com.rimuru.android.rhythmlens.ui.screens.home.HomeRoute
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

fun NavGraphBuilder.bottomNavGraph(
    rootNavController: NavHostController
) {
    composable<BottomNavDestination.Home> {
        HomeRoute(
            onNavigateToScan = {
                rootNavController.navigate(ScanDestination)
            },
            onOpenGalleryPicker = {
                // TODO: открыть системный Photo Picker
            },
            onOpenFilePicker = {
                // TODO: открыть системный File Picker
            },
            onNavigateToEcgDetail = { ecgId ->
                rootNavController.navigate(
                    EcgDetailDestination(ecgId)
                )
            }
        )
    }

    composable<BottomNavDestination.History> {
        HistoryScreen(
            onEcgClick = { ecgId ->
                rootNavController.navigate(
                    EcgDetailDestination(ecgId)
                )
            }
        )
    }

    composable<BottomNavDestination.Patients> {
        PlaceholderScreen(
            title = stringResource(R.string.patients)
        )
    }

    composable<BottomNavDestination.Profile> {
        PlaceholderScreen(
            title = stringResource(R.string.profile)
        )
    }
}

@Composable
private fun PlaceholderScreen(
    title: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(RhythmSpacing.XLarge),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.placeholder_screen_template, title),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
