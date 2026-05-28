package com.rimuru.android.rhythmlens.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rimuru.android.rhythmlens.ui.navigation.BottomNavDestination
import com.rimuru.android.rhythmlens.ui.navigation.BottomNavigationBar
import com.rimuru.android.rhythmlens.ui.navigation.bottomNavGraph

@Composable
fun MainScreen(
    rootNavController: NavHostController
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    bottomNavController.navigate(destination) {
                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                            saveState = true
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavDestination.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            bottomNavGraph(
                rootNavController = rootNavController
            )
        }
    }
}