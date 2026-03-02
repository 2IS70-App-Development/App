package app.cryptoseal.feature.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.cryptoseal.core.navigation.BottomNavItem
import app.cryptoseal.feature.packages.ReceivingListScreen
import app.cryptoseal.feature.packages.SendingListScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cryptoseal.feature.packages.PackagesViewModel
import app.cryptoseal.feature.packages.CreatorScreen
import app.cryptoseal.feature.profile.ProfileScreen

@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    // This NavController manages the tabs inside the dashboard
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = { CryptoSealBottomNavigationBar(bottomNavController) }
    ) { innerPadding ->
        // The Box contains the actual screen content, pushed down by the Scaffold padding
        Box(modifier = Modifier.padding(innerPadding)) {
            DashboardNavGraph(navController = bottomNavController, onLogout = onLogout)
        }
    }
}

@Composable
fun CryptoSealBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Sending,
        BottomNavItem.Receiving,
        BottomNavItem.Creator,
        BottomNavItem.Scanner,
        BottomNavItem.Profile
    )

    NavigationBar(
        // This forces the bar's background to use your custom Dark Navy
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val icon = when (item) {
                BottomNavItem.Sending -> Icons.Default.List
                BottomNavItem.Receiving -> Icons.Default.Call
                BottomNavItem.Creator -> Icons.Default.Add
                BottomNavItem.Scanner -> Icons.Default.Search
                BottomNavItem.Profile -> Icons.Default.Person
                else -> Icons.Default.List
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
                // This block explicitly overrides the default M3 colors
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary, // Cyan icon
                    selectedTextColor = MaterialTheme.colorScheme.primary, // Cyan text
                    indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), // Subtle Steel Blue pill
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), // Dimmed white
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
fun DashboardNavGraph(navController: NavHostController, onLogout: () -> Unit) {
    // Instantiate the shared ViewModel here
    val sharedPackagesViewModel: PackagesViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Sending.route
    ) {
        composable(BottomNavItem.Sending.route) {
            SendingListScreen(viewModel = sharedPackagesViewModel)
        }
        composable(BottomNavItem.Receiving.route) {
            ReceivingListScreen()
        }
        composable(BottomNavItem.Creator.route) {
            CreatorScreen(viewModel = sharedPackagesViewModel)
        }
        composable(BottomNavItem.Scanner.route) {
            app.cryptoseal.feature.scanner.ScannerScreen()
        }
        composable(BottomNavItem.Profile.route) { ProfileScreen(onLogout = onLogout) }
    }
}