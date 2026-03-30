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
 * The main Dashboard screen for the authenticated user session.
 * 
 * This screen acts as the primary container for the application's core functionality, 
 * using a Bottom Navigation pattern to switch between different feature tabs.
 *
 * @param onLogout A callback function triggered when the user initiates a logout 
 * (typically from the Profile tab), leading back to the Login screen.
 */
@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    // NavController dedicated to managing navigation between bottom bar tabs.
    val bottomNavController = rememberNavController()

    Scaffold(
        // The persistent bottom bar that remains visible as the user switches tabs.
        bottomBar = { CryptoSealBottomNavigationBar(bottomNavController) }
    ) { innerPadding ->
        // The Box acts as a content area for the current tab, respecting Scaffold's padding (e.g., bottom bar height).
        Box(modifier = Modifier.padding(innerPadding)) {
            // The NavGraph defines which Composable is shown based on the current 'bottomNavController' route.
            DashboardNavGraph(navController = bottomNavController, onLogout = onLogout)
        }
    }
}

/**
 * A custom Bottom Navigation Bar implementation for the CryptoSeal app.
 *
 * It dynamically highlights the selected item and performs optimized navigation 
 * (preserving state and avoiding multiple instances of the same tab).
 *
 * @param navController The NavController that tracks the state of the dashboard tabs.
 */
@Composable
fun CryptoSealBottomNavigationBar(navController: NavHostController) {
    // The list of navigation items defined in Navigation.kt
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
        // Observe the current back stack entry to determine which tab is currently active.
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            // Match icons to the corresponding bottom navigation items.
            val icon = when (item) {
                BottomNavItem.Packages -> Icons.Default.Home
                BottomNavItem.Activity -> Icons.Default.Notifications
                BottomNavItem.Creator -> Icons.Default.Add
                BottomNavItem.Scanner -> Icons.Default.QrCodeScanner
                BottomNavItem.Profile -> Icons.Default.Person
            }

            NavigationBarItem(
                icon = { Icon(imageVector = icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    // Optimized navigation logic:
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to avoid building up a large stack of destinations.
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) { saveState = true }
                        }
                        // Avoid multiple copies of the same destination when reselecting the same item.
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item.
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
 * The inner Navigation Graph for the Dashboard.
 * 
 * Maps specific routes to their corresponding Tab Composables. 
 * ViewModels are instantiated here to control their lifecycle relative to the Dashboard.
 *
 * @param navController The controller used for tab switching.
 * @param onLogout Callback for logout events.
 */
@Composable
fun DashboardNavGraph(navController: NavHostController, onLogout: () -> Unit) {
    // PackagesViewModel is shared between 'Packages' and 'Creator' tabs 
    // to ensure that creating a package immediately updates the main list.
    val sharedPackagesViewModel: PackagesViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Packages.route
    ) {
        // Route for the list of packages (sent and received).
        composable(BottomNavItem.Packages.route) {
            PackagesTab(viewModel = sharedPackagesViewModel)
        }

        // Route for the activity feed / notifications.
        composable(BottomNavItem.Activity.route) {
            val activityViewModel: ActivityViewModel = viewModel()
            ActivityTab(viewModel = activityViewModel)
        }

        // Route for the order creation screen.
        composable(BottomNavItem.Creator.route) {
            CreatorTab(
                creatorViewModel = viewModel(),
                packagesViewModel = sharedPackagesViewModel,
                onFinish = {
                    // After successfully creating an order, navigate back to the Packages tab.
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

        // Route for the QR scanner.
        composable(BottomNavItem.Scanner.route) {
            ScannerTab()
        }

        // Route for user profile, contact management, and logout.
        composable(BottomNavItem.Profile.route) {
            ProfileTab(onLogout = onLogout)
        }
    }
}
