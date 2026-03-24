package app.cryptoseal.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    fun formatIsoDate(isoString: String): String {
        return try {
            val parsed = ZonedDateTime.parse(isoString)
            val formatter =
                DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            parsed.format(formatter)
        } catch (e: Exception) {
            isoString
        }
    }
}
