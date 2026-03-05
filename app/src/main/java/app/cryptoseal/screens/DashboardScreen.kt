package app.cryptoseal.feature.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.cryptoseal.BottomNavItem
import app.cryptoseal.tabs.activity.ActivityTab
import app.cryptoseal.tabs.creator.CreatorTab
import app.cryptoseal.tabs.scanner.ScannerTab
import app.cryptoseal.tabs.PackagesViewModel
import app.cryptoseal.tabs.packages.PackagesTab
import app.cryptoseal.tabs.profile.ProfileTab

@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = { CryptoSealBottomNavigationBar(bottomNavController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            DashboardNavGraph(navController = bottomNavController, onLogout = onLogout)
        }
    }
}

@Composable
fun CryptoSealBottomNavigationBar(navController: NavHostController) {
    // 1. Updated list of tabs
    val items = listOf(
        BottomNavItem.Packages,
        BottomNavItem.Activity,
        BottomNavItem.Creator,
        BottomNavItem.Scanner,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            // 2. Updated Icon Logic
            val icon = when (item) {
                BottomNavItem.Packages -> Icons.Default.Home
                BottomNavItem.Activity -> Icons.Default.Notifications
                BottomNavItem.Creator -> Icons.Default.Add
                BottomNavItem.Scanner -> Icons.Default.Search
                BottomNavItem.Profile -> Icons.Default.Person
                else -> Icons.Default.Home
            }

            NavigationBarItem(
                icon = { Icon(imageVector = icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
fun DashboardNavGraph(navController: NavHostController, onLogout: () -> Unit) {
    val sharedPackagesViewModel: PackagesViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Packages.route // New Start Destination
    ) {
        // 3. Updated Graph Destinations
        composable(BottomNavItem.Packages.route) {
            PackagesTab(viewModel = sharedPackagesViewModel)
        }
        composable(BottomNavItem.Activity.route) {
            ActivityTab()
        }
        composable(BottomNavItem.Creator.route) {
            CreatorTab(viewModel = sharedPackagesViewModel)
        }
        composable(BottomNavItem.Scanner.route) {
            ScannerTab()
        }
        composable(BottomNavItem.Profile.route) {
            ProfileTab(onLogout = onLogout)
        }
    }
}