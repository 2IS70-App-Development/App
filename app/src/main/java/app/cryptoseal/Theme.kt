package app.cryptoseal

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * CryptoSeal Brand Colors
 * 
 * These colors are derived from the application's logo to ensure brand consistency.
 * We focus on a dark, high-contrast theme to convey security and professionalism.
 */

// Primary Brand Color: A vibrant cyan used for buttons, active states, and focus indicators.
private val CryptoSealCyan = Color(0xFF5CE1D6)

// Secondary Color: A muted steel blue used for secondary buttons and less emphasized UI elements.
private val CryptoSealSteelBlue = Color(0xFF418B9F)

// Background Color: A deep, dark navy that serves as the primary canvas for the app.
private val CryptoSealDarkNavy = Color(0xFF09141E)

// Surface Color: A slightly lighter navy used for Cards, Dialogs, and Bottom Sheets 
// to create visual depth and separation from the background.
private val CryptoSealSurfaceNavy = Color(0xFF132738)

// Text Color: A very light cyan-tinted white to ensure maximum readability on dark surfaces.
private val CryptoSealTextWhite = Color(0xFFE5F7F5)

/**
 * DarkColorPalette defines the Material 3 ColorScheme mapping for CryptoSeal.
 * 
 * This follows the M3 specification where 'onSurface', 'onPrimary', etc., 
 * define the color of content (text/icons) sitting on top of those surfaces.
 */
private val DarkColorPalette = darkColorScheme(
    primary = CryptoSealCyan,
    secondary = CryptoSealSteelBlue,
    background = CryptoSealDarkNavy,
    surface = CryptoSealSurfaceNavy,

    // Text/Icon colors on different backgrounds
    onPrimary = Color.Black, // Black text on Cyan buttons for contrast
    onSecondary = Color.White,
    onBackground = CryptoSealTextWhite,
    onSurface = CryptoSealTextWhite,

    // Tertiary can be used for accents like badges or special status indicators
    tertiary = Color(0xFFE91E63) // A vibrant pink for errors/warnings
)

/**
 * CryptoSealTheme is the root Composable for styling the application.
 * 
 * It wraps the MaterialTheme with our custom color scheme. Currently, 
 * it only implements a dark theme to align with the application's aesthetic.
 * 
 * Usage:
 * CryptoSealTheme {
 *    // Your app content here
 * }
 *
 * @param content The UI content that will inherit this theme's styling.
 */
@Composable
fun CryptoSealTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorPalette,
        // Typography and Shapes could also be customized here if needed.
        content = content
    )
}
