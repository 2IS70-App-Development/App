package app.cryptoseal

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Dashboard : Screen("dashboard_screen")
}

sealed class BottomNavItem(val route: String, val title: String) {
    object Packages : BottomNavItem("packages", "Packages")
    object Activity : BottomNavItem("activity", "Activity")
    object Creator : BottomNavItem("creator", "Creator")
    object Scanner : BottomNavItem("scanner", "Scanner")
    object Profile : BottomNavItem("profile", "Profile")

    // Removed: Sending, Receiving
}