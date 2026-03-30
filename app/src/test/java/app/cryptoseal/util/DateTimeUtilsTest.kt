package app.cryptoseal.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DateTimeUtilsTest {

    @Test
    fun `formatIsoDate with valid ISO string returns formatted date`() {
        // Set default locale to US for consistent test results
        Locale.setDefault(Locale.US)

        val input = "2023-10-27T10:15:30Z"
        // Expected format: "MMM dd, yyyy • hh:mm a"
        val expected = "Oct 27, 2023 • 10:15 AM"

        val result = DateTimeUtils.formatIsoDate(input)

        assertEquals(expected, result)
    }

    @Test
    fun `formatIsoDate with PM time returns formatted date`() {
        Locale.setDefault(Locale.US)

        val input = "2023-10-27T22:15:30Z"
        val expected = "Oct 27, 2023 • 10:15 PM"

        val result = DateTimeUtils.formatIsoDate(input)

        assertEquals(expected, result)
    }

    @Test
    fun `formatIsoDate with invalid string returns original string`() {
        val input = "not-a-date"
        val result = DateTimeUtils.formatIsoDate(input)

        assertEquals(input, result)
    }

    @Test
    fun `formatIsoDate with empty string returns original string`() {
        val input = ""
        val result = DateTimeUtils.formatIsoDate(input)

        assertEquals(input, result)
    }
}
