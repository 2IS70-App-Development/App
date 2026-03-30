package app.cryptoseal.feature.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
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
import app.cryptoseal.tabs.PackagesViewModel
import app.cryptoseal.tabs.activity.ActivityTab
import app.cryptoseal.tabs.activity.ActivityViewModel
import app.cryptoseal.tabs.creator.CreatorTab
import app.cryptoseal.tabs.packages.PackagesTab
import app.cryptoseal.tabs.profile.ProfileTab
import app.cryptoseal.tabs.scanner.ScannerTab

/**
 * The primary container for the authenticated part of the application.
 * It sets up the bottom navigation bar and the inner navigation host that
 * switches between different tabs (Packages, Activity, Creator, Scanner, Profile).
 *
 * @param onLogout Callback to be triggered when the user logs out from the Profile tab.
 */
@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    // NavController for the internal dashboard navigation.
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = { CryptoSealBottomNavigationBar(bottomNavController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // Main navigation host for the dashboard tabs.
            DashboardNavGraph(navController = bottomNavController, onLogout = onLogout)
        }
    }
}

/**
 * The bottom navigation bar for the CryptoSeal app.
 * Dynamically highlights the currently active route and handles tab switching.
 *
 * @param navController The navigation controller used to perform tab transitions.
 */
@Composable
fun CryptoSealBottomNavigationBar(navController: NavHostController) {
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
            // Resolve the icon for each bottom navigation item.
            val icon = when (item) {
                BottomNavItem.Packages -> Icons.Default.Home
                BottomNavItem.Activity -> Icons.Default.Notifications
                BottomNavItem.Creator -> Icons.Default.Add
                BottomNavItem.Scanner -> Icons.Default.QrCodeScanner
                BottomNavItem.Profile -> Icons.Default.Person
                else -> Icons.Default.Home
            }

            NavigationBarItem(
                icon = { Icon(imageVector = icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    // Navigate to the selected tab, popping up to the start destination to avoid stack buildup.
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

/**
 * Defines the navigation graph for the dashboard tabs.
 * Manages the lifecycle of ViewModels used within these tabs.
 *
 * @param navController The navigation controller managing the tab stack.
 * @param onLogout Passed down to the Profile tab.
 */
@Composable
fun DashboardNavGraph(navController: NavHostController, onLogout: () -> Unit) {
    // PackagesViewModel is shared between the Packages list and the Creator tab
    // to allow immediate UI updates when a new package is created.
    val sharedPackagesViewModel: PackagesViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Packages.route
    ) {
        // Tab: Package Tracking
        composable(BottomNavItem.Packages.route) {
            PackagesTab(viewModel = sharedPackagesViewModel)
        }

        // Tab: Recent Activity/Notifications
        composable(BottomNavItem.Activity.route) {
            val activityViewModel: ActivityViewModel = viewModel()
            ActivityTab(viewModel = activityViewModel)
        }

        // Tab: Create New Shipment
        composable(BottomNavItem.Creator.route) {
            CreatorTab(
                creatorViewModel = viewModel(),
                packagesViewModel = sharedPackagesViewModel,
                onFinish = {
                    // Navigate back to the packages list once an order is successfully created.
                    navController.navigate(BottomNavItem.Packages.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Tab: QR Scanner for Package Custody Updates
        composable(BottomNavItem.Scanner.route) {
            ScannerTab()
        }

        // Tab: User Profile and Settings
        composable(BottomNavItem.Profile.route) {
            ProfileTab(onLogout = onLogout)
        }
    }
}
