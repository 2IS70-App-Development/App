package app.cryptoseal

/**
 * Represents the different top-level screens in the application's main navigation graph.
 * @property route The unique identifier used for navigation routing.
 */
sealed class Screen(val route: String) {
    /** The authentication entry point. */
    object Login : Screen("login_screen")

    /** The primary authenticated container for the application. */
    object Dashboard : Screen("dashboard_screen")
}

/**
 * Represents the items within the bottom navigation bar of the Dashboard.
 * @property route The navigation route associated with the tab.
 * @property title The display label for the tab.
 */
sealed class BottomNavItem(val route: String, val title: String) {
    /** Tab for viewing and managing package orders. */
    object Packages : BottomNavItem("packages", "Packages")

    /** Tab for viewing recent application activities and notifications. */
    object Activity : BottomNavItem("activity", "Activity")

    /** Tab for creating new package orders. */
    object Creator : BottomNavItem("creator", "Creator")

    /** Tab for scanning package QR codes and updating status. */
    object Scanner : BottomNavItem("scanner", "Scanner")

    /** Tab for managing user profile and settings. */
    object Profile : BottomNavItem("profile", "Profile")
}