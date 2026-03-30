package app.cryptoseal

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors extracted from the CryptoSeal logo
private val CryptoSealCyan = Color(0xFF5CE1D6)        // Bright cyan for primary buttons/accents
private val CryptoSealSteelBlue = Color(0xFF418B9F)   // Muted steel blue for secondary elements
private val CryptoSealDarkNavy = Color(0xFF09141E)    // Very dark navy for the main background
private val CryptoSealSurfaceNavy = Color(0xFF132738) // Slightly lighter navy for cards and dialogs
private val CryptoSealTextWhite = Color(0xFFE5F7F5)   // Cyan-tinted white for text readability

/**
 * The primary dark color scheme for the CryptoSeal application.
 * Utilizes a professional navy and cyan palette for a modern, secure look.
 */
private val DarkColorPalette = darkColorScheme(
    primary = CryptoSealCyan,
    secondary = CryptoSealSteelBlue,
    background = CryptoSealDarkNavy,
    surface = CryptoSealSurfaceNavy,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = CryptoSealTextWhite,
    onSurface = CryptoSealTextWhite
)

/**
 * CryptoSealTheme provides the Material 3 styling for the entire application.
 * It currently only supports a Dark mode to match the application's branding.
 *
 * @param content The composable content to be styled by this theme.
 */
@Composable
fun CryptoSealTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorPalette,
        content = content
    )
}