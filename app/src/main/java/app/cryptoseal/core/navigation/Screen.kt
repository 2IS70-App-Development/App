package app.cryptoseal.core.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Dashboard : Screen("dashboard_screen")
}

sealed class BottomNavItem(val route: String, val title: String) {
    object Sending : BottomNavItem("sending_list", "Sending")
    object Receiving : BottomNavItem("receiving_list", "Receiving")
    object Creator : BottomNavItem("qr_creator", "Creator")
    object Scanner : BottomNavItem("qr_scanner", "Scanner")
    object Profile : BottomNavItem("profile_settings", "Profile")
}