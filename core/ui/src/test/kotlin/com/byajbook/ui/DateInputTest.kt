package com.byajbook.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateInputTest {

    @Test
    fun testShortDateExpansion() {
        // (1) “8/5/26” → LocalDate(2026,5,8) -> "08052026"
        assertEquals("08052026", processShortDateOrValidate("8/5/26").digitsOnly)
        assertFalse(processShortDateOrValidate("8/5/26").isInvalid)

        // (2) “8-5-26” → LocalDate(2026,5,8)
        assertEquals("08052026", processShortDateOrValidate("8-5-26").digitsOnly)

        // (3) “1/1/27” → LocalDate(2027,1,1)
        assertEquals("01012027", processShortDateOrValidate("1/1/27").digitsOnly)

        // (4) “31/12/99” → LocalDate(2099,12,31)
        assertEquals("31122099", processShortDateOrValidate("31/12/99").digitsOnly)

        // (5) “32/5/26” → “Invalid date” error
        assertTrue(processShortDateOrValidate("32/5/26").isInvalid)

        // (6) “8/13/26” → “Invalid date” error
        assertTrue(processShortDateOrValidate("8/13/26").isInvalid)
    }

    @Test
    fun testFullDateValidation() {
        // Valid 8 digits
        assertFalse(processShortDateOrValidate("01012026").isInvalid)
        assertEquals("01012026", processShortDateOrValidate("01012026").digitsOnly)

        // Invalid month
        assertTrue(processShortDateOrValidate("01132026").isInvalid)

        // Year out of range
        assertTrue(processShortDateOrValidate("01011899").isInvalid)
        assertTrue(processShortDateOrValidate("01012101").isInvalid)
    }

    @Test
    fun testFormatDateInput() {
        // [FIX-DATEINPUT-STEP1-GATE-1]
        assertEquals("", formatDateInput(""))
        assertEquals("2", formatDateInput("2"))
        assertEquals("23/", formatDateInput("23"))
        assertEquals("23/0", formatDateInput("230"))
        assertEquals("23/04/", formatDateInput("2304"))
        assertEquals("23/04/2", formatDateInput("23042"))
        assertEquals("23/04/2026", formatDateInput("23042026"))
        
        // Truncate to 8 digits before formatting
        assertEquals("23/04/2026", formatDateInput("230420261"))
    }
}
