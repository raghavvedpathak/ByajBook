package com.byajbook.data.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TypeConverterTest {
    private val converters = AppTypeConverters()

    @Test
    fun testLocalDateRoundTrip() {
        val date = LocalDate.of(2026, 4, 23)
        val string = converters.fromLocalDate(date)
        assertEquals("2026-04-23", string)
        assertEquals(date, converters.toLocalDate(string))
    }

    @Test
    fun testLocalDateTimeRoundTrip() {
        // [FIX-TIMESTAMP-STEP2-1] Verify time component is preserved
        val dateTime = LocalDateTime.of(2026, 4, 23, 14, 30, 45)
        val string = converters.fromLocalDateTime(dateTime)
        assertEquals("2026-04-23T14:30:45", string)
        
        val restored = converters.toLocalDateTime(string)
        assertEquals(dateTime, restored)
        assertEquals(14, restored?.hour)
        assertEquals(30, restored?.minute)
        assertEquals(45, restored?.second)
    }

    @Test
    fun testNullableHandling() {
        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
        assertNull(converters.fromLocalDateTime(null))
        assertNull(converters.toLocalDateTime(null))
    }

    @Test
    fun testRunCatchingGuard() {
        // Bad strings should return null, not crash
        assertNull(converters.toLocalDate("not-a-date"))
        assertNull(converters.toLocalDateTime("not-a-datetime"))
    }
}
