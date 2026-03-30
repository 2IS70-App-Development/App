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
 * MainActivity serves as the single entry point for the CryptoSeal Android application.
 * 
 * As a Modern Android Development (MAD) app, it utilizes Jetpack Compose for the UI 
 * and a single-activity architecture. It handles the top-level navigation flow, 
 * switching between the authentication layer and the main application dashboard.
 */
class MainActivity : ComponentActivity() {

    /**
     * Standard Android Activity lifecycle method.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously 
     * being shut down then this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setContent is the entry point for Jetpack Compose UI
        setContent {
            // Apply the global application theme (defined in Theme.kt)
            CryptoSealTheme {
                // Surface is a fundamental Compose container that provides background color and content color
                Surface(
                    // fillMaxSize makes the surface cover the entire screen area
                    modifier = Modifier.fillMaxSize(),
                    // Use the background color defined in our custom color scheme
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Start the root navigation graph of the application
                    CryptoSealNavGraph()
                }
            }
        }
    }
}

/**
 * CryptoSealNavGraph manages the high-level navigation routes of the app.
 * 
 * This composable uses the Jetpack Navigation Component to define the relationship 
 * between the Login screen and the Dashboard. It also implements the initial 
 * routing logic based on the user's current authentication status.
 */
@Composable
fun CryptoSealNavGraph() {
    // rememberNavController() creates and persists the NavController across recompositions
    val navController = rememberNavController()

    // Authentication Logic:
    // We check the ApiService to see if a valid session exists.
    // If logged in, we skip the Login screen and go straight to the Dashboard.
    val startDestination = if (ApiService.isLoggedIn()) {
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }

    // NavHost is the container where navigation actually happens.
    // It maps routes (strings) to Composable functions.
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        /**
         * Route for the Login / Authentication screen.
         */
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Logic triggered after a successful login or signup:
                    // Navigate to the Dashboard.
                    navController.navigate(Screen.Dashboard.route) {
                        // Crucial: We remove the Login screen from the backstack (popUpTo)
                        // so that pressing 'Back' from the Dashboard doesn't return to Login.
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        /**
         * Route for the Main Dashboard (the core of the app).
         */
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onLogout = {
                    // Logic triggered when the user clicks 'Logout' in their profile:
                    // Redirect back to the Login screen.
                    navController.navigate(Screen.Login.route) {
                        // Again, we clear the Dashboard from history to prevent 'Back' navigation issues.
                        popUpTo(Screen.Dashboard.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
