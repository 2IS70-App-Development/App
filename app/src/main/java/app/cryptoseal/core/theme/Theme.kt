package app.cryptoseal.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define your dark mode colors here
private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFFBB86FC),      // Light purple for primary buttons/accents
    secondary = Color(0xFF03DAC5),    // Teal for secondary elements
    background = Color(0xFF121212),   // Very dark gray for the main background
    surface = Color(0xFF1E1E1E),      // Slightly lighter gray for cards and dialogs
    onPrimary = Color.Black,          // Text color on top of primary color
    onSecondary = Color.Black,        // Text color on top of secondary color
    onBackground = Color.White,       // Text color on the main background
    onSurface = Color.White           // Text color on surface elements
)

@Composable
fun CryptoSealTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorPalette,
        content = content
    )
}