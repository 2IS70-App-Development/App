package app.cryptoseal

/**
 * Screen sealed class defines the top-level navigation routes for the main NavHost.
 * 
 * Each object represents a distinct destination in the app that isn't part of 
 * the bottom navigation (e.g., the Login flow vs. the entire Dashboard container).
 * 
 * @property route The unique string identifier used by the Navigation Component.
 */
sealed class Screen(val route: String) {
    /** 
     * The authentication entry point. This screen handles both login and signup 
     * logic before the user enters the main app.
     */
    object Login : Screen("login_screen")

    /** 
     * The main application container. Once authenticated, the user resides here.
     * This screen contains its own internal NavHost for switching between tabs.
     */
    object Dashboard : Screen("dashboard_screen")
}

/**
 * BottomNavItem sealed class defines the routes for the Dashboard's bottom navigation bar.
 * 
 * Each object corresponds to one of the five tabs visible at the bottom of the screen.
 * 
 * @property route The navigation route used to identify the tab.
 * @property title The human-readable label displayed under the icon in the navigation bar.
 */
sealed class BottomNavItem(val route: String, val title: String) {

    /** 
     * Home tab: Displays lists of sent and received packages. 
     * Allows users to see their ongoing shipments.
     */
    object Packages : BottomNavItem("packages", "Packages")

    /** 
     * Activity tab: Shows a chronological feed of notifications and system events 
     * relevant to the user (e.g., "Order #123 was scanned").
     */
    object Activity : BottomNavItem("activity", "Activity")

    /** 
     * Creator tab: The 'plus' button screen where users can start a new shipment 
     * by entering details and generating a new QR code.
     */
    object Creator : BottomNavItem("creator", "Creator")

    /** 
     * Scanner tab: Activates the camera to scan package QR codes and record 
     * handovers with GPS location and condition data.
     */
    object Scanner : BottomNavItem("scanner", "Scanner")

    /** 
     * Profile tab: Manages the user's account details, contact list, 
     * and provides the logout functionality.
     */
    object Profile : BottomNavItem("profile", "Profile")
}
