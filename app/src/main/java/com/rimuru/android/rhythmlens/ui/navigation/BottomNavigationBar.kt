package com.rimuru.android.rhythmlens.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rimuru.android.rhythmlens.R

@Composable
fun BottomNavigationBar(
    currentDestination: BottomNavDestination,
    onNavigate: (BottomNavDestination) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination is BottomNavDestination.Home,
            onClick = { onNavigate(BottomNavDestination.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.home)) }
        )

        NavigationBarItem(
            selected = currentDestination is BottomNavDestination.History,
            onClick = { onNavigate(BottomNavDestination.History) },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text(stringResource(R.string.history)) }
        )

        NavigationBarItem(
            selected = currentDestination is BottomNavDestination.Patients,
            onClick = { onNavigate(BottomNavDestination.Patients) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text(stringResource(R.string.patients)) }
        )

        NavigationBarItem(
            selected = currentDestination is BottomNavDestination.Profile,
            onClick = { onNavigate(BottomNavDestination.Profile) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text(stringResource(R.string.profile)) }
        )
    }
}