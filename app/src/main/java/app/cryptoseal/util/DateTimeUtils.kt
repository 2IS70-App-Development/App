package app.cryptoseal.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility object for handling date and time formatting across the application.
 */
object DateTimeUtils {
    /**
     * Parses an ISO 8601 date string and formats it into a human-readable string.
     * Example input: "2023-10-27T10:15:30Z"
     * Example output: "Oct 27, 2023 • 10:15 AM"
     *
     * @param isoString The ISO 8601 formatted date string from the API.
     * @return A formatted string for display, or the original string if parsing fails.
     */
    fun formatIsoDate(isoString: String): String {
        return try {
            val parsed = ZonedDateTime.parse(isoString)
            // Pattern: Month Abbreviation, Day, Year • Hour:Minute AM/PM
            val formatter =
                DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            parsed.format(formatter)
        } catch (e: Exception) {
            // Fallback to the original string if it doesn't match the expected ISO format.
            isoString
        }
    }
}
