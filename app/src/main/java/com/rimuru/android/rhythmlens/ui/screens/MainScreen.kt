package com.rimuru.android.rhythmlens.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.navigation.BottomNavDestination
import com.rimuru.android.rhythmlens.ui.navigation.EcgDetailDestination
import com.rimuru.android.rhythmlens.ui.screens.history.HistoryScreen
import com.rimuru.android.rhythmlens.ui.screens.home.HomeScreen

@Composable
fun MainScreen(
    rootNavController: NavHostController
) {

    // ОТДЕЛЬНЫЙ controller для bottom navigation
    val bottomNavController = rememberNavController()

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()

    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavDestination.Home,
        BottomNavDestination.History,
        BottomNavDestination.Patients,
        BottomNavDestination.Profile
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),

        bottomBar = {

            NavigationBar {

                bottomNavItems.forEach { destination ->

                    val selected =
                        currentDestination?.hierarchy?.any {
                            it.route == destination::class.qualifiedName
                        } == true

                    NavigationBarItem(
                        selected = selected,

                        onClick = {

                            bottomNavController.navigate(destination) {

                                popUpTo(
                                    bottomNavController.graph.findStartDestination().id
                                ) {
                                    saveState = true
                                }

                                launchSingleTop = true

                                restoreState = true
                            }
                        },

                        icon = {

                            when (destination) {

                                is BottomNavDestination.Home -> {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null
                                    )
                                }

                                is BottomNavDestination.History -> {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null
                                    )
                                }

                                is BottomNavDestination.Patients -> {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null
                                    )
                                }

                                is BottomNavDestination.Profile -> {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null
                                    )
                                }
                            }
                        },

                        label = {

                            when (destination) {

                                is BottomNavDestination.Home -> {
                                    Text(stringResource(R.string.home))
                                }

                                is BottomNavDestination.History -> {
                                    Text(stringResource(R.string.history))
                                }

                                is BottomNavDestination.Patients -> {
                                    Text(stringResource(R.string.patients))
                                }

                                is BottomNavDestination.Profile -> {
                                    Text(stringResource(R.string.profile))
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        BottomNavHost(
            rootNavController = rootNavController,
            bottomNavController = bottomNavController,
            innerPadding = innerPadding
        )
    }
}

@Composable
private fun BottomNavHost(
    rootNavController: NavHostController,
    bottomNavController: NavHostController,
    innerPadding: PaddingValues
) {

    NavHost(
        navController = bottomNavController,

        startDestination = BottomNavDestination.Home,

        modifier = Modifier.padding(innerPadding)
    ) {

        composable<BottomNavDestination.Home> {

            HomeScreen(
                onScanClick = { },
                onGalleryClick = { },
                onImportClick = { }
            )
        }

        composable<BottomNavDestination.History> {

            HistoryScreen(

                onEcgClick = { ecgId ->

                    // используем ROOT controller для detail screen
                    rootNavController.navigate(
                        EcgDetailDestination(ecgId)
                    )
                },

                onCompareClick = { }
            )
        }

        composable<BottomNavDestination.Patients> {

            Text("Patients Screen")
        }

        composable<BottomNavDestination.Profile> {

            Text("Profile Screen")
        }
    }
}