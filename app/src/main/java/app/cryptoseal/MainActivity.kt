package app.cryptoseal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.cryptoseal.data.api.ApiService
import app.cryptoseal.feature.auth.LoginScreen
import app.cryptoseal.feature.dashboard.DashboardScreen

/**
 * The main entry point of the CryptoSeal application.
 * This activity sets up the Compose theme and the primary navigation graph.
 */
class MainActivity : ComponentActivity() {
    /**
     * Called when the activity is starting. This is where most initialization should go.
     * It sets the content of the activity to the CryptoSealTheme and the navigation graph.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Apply the custom application theme
            CryptoSealTheme {
                // Surface provides a background color for the application
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Host the application's navigation logic
                    CryptoSealNavGraph()
                }
            }
        }
    }
}

/**
 * Defines the application's top-level navigation structure using Jetpack Compose Navigation.
 * It manages the transitions between the Login screen and the Dashboard.
 */
@Composable
fun CryptoSealNavGraph() {
    // Initialise the NavController to manage app navigation
    val navController = rememberNavController()

    // Determine the starting screen based on the user's login status
    val startDestination = if (ApiService.isLoggedIn()) Screen.Dashboard.route else Screen.Login.route

    // Set up the NavHost with defined routes
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen Destination: Shown when the user is not authenticated.
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // On success, navigate to Dashboard and clear the login screen from history.
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Main Dashboard Destination: The primary UI after successful authentication.
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onLogout = {
                    // On logout, navigate back to the Login screen and clear the dashboard from history.
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
