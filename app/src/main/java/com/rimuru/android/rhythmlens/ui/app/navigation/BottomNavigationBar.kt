package com.rimuru.android.rhythmlens.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.domain.model.UserRole

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    role: UserRole?,
    onNavigate: (BottomNavDestination) -> Unit
) {
    val items = buildList {
        add(
            BottomNavItem(
                destination = BottomNavDestination.Home,
                route = BottomNavDestination.Home::class.qualifiedName,
                label = stringResource(R.string.home),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null
                    )
                }
            )
        )

        add(
            BottomNavItem(
                destination = BottomNavDestination.History,
                route = BottomNavDestination.History::class.qualifiedName,
                label = stringResource(R.string.history),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null
                    )
                }
            )
        )

        if (role == UserRole.DOCTOR) {
            add(
                BottomNavItem(
                    destination = BottomNavDestination.Patients,
                    route = BottomNavDestination.Patients::class.qualifiedName,
                    label = stringResource(R.string.patients),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null
                        )
                    }
                )
            )
        }

        add(
            BottomNavItem(
                destination = BottomNavDestination.Profile,
                route = BottomNavDestination.Profile::class.qualifiedName,
                label = stringResource(R.string.profile),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null
                    )
                }
            )
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.destination) },
                icon = item.icon,
                label = {
                    Text(text = item.label)
                }
            )
        }
    }
}

private data class BottomNavItem(
    val destination: BottomNavDestination,
    val route: String?,
    val label: String,
    val icon: @Composable () -> Unit
)
