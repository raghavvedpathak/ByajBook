package com.byajbook.calculations

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InterestEngineTest {

    @Test
    fun testGetMonthsBetween_Rollover_Jan31_Mar5() {
        // rollover: dayDiff=-26, adjustedDays=2 (Feb has 28 days) -> +0.5 month
        val start = LocalDate.of(2023, 1, 31)
        val end = LocalDate.of(2023, 3, 5)
        assertEquals(1.5, getMonthsBetween(start, end), 0.0)
    }

    @Test
    fun testGetMonthsBetween_SameDay() {
        val start = LocalDate.of(2023, 1, 1)
        val end = LocalDate.of(2023, 1, 1)
        assertEquals(0.0, getMonthsBetween(start, end), 0.0)
    }

    @Test
    fun testGetMonthsBetween_PositiveBranch_15Days() {
        // Jan-1 -> Jan-16: dayDiff=15, passes >0 test -> +0.5 month
        val start = LocalDate.of(2023, 1, 1)
        val end = LocalDate.of(2023, 1, 16)
        assertEquals(0.5, getMonthsBetween(start, end), 0.0)
    }

    @Test
    fun testGetMonthsBetween_PositiveBranch_16Days() {
        // Jan-1 -> Jan-17: dayDiff=16 (>15) -> +1.0 month
        val start = LocalDate.of(2023, 1, 1)
        val end = LocalDate.of(2023, 1, 17)
        assertEquals(1.0, getMonthsBetween(start, end), 0.0)
    }

    @Test
    fun testGetMonthsBetween_NextMonthStart() {
        val start = LocalDate.of(2023, 1, 1)
        val end = LocalDate.of(2023, 2, 1)
        assertEquals(1.0, getMonthsBetween(start, end), 0.0)
    }

    @Test
    fun testGetMonthsBetween_Rollover_Jan31_Feb28() {
        // Jan-31 -> Feb-28: dayDiff=-3, adjustedDays=28 (31-31+28), 28>15 -> +1.0 month
        val start = LocalDate.of(2023, 1, 31)
        val end = LocalDate.of(2023, 2, 28)
        assertEquals(1.0, getMonthsBetween(start, end), 0.0)
    }

    @Test
    fun testGetMonthsBetween_CrossYearRollover() {
        // Dec-15 -> Jan-1: dayDiff=-14, adjustedDays=17 (31-15+1), 17>15 -> +1.0 month
        val start = LocalDate.of(2022, 12, 15)
        val end = LocalDate.of(2023, 1, 1)
        assertEquals(1.0, getMonthsBetween(start, end), 0.0)
    }

    @Test
    fun testGetMonthsBetween_FullYear() {
        val start = LocalDate.of(2023, 1, 1)
        val end = LocalDate.of(2024, 1, 1)
        assertEquals(12.0, getMonthsBetween(start, end), 0.0)
    }

    @Test
    fun testGetMonthsBetween_SameDayMonth() {
        val start = LocalDate.of(2023, 3, 5)
        val end = LocalDate.of(2023, 3, 5)
        assertEquals(0.0, getMonthsBetween(start, end), 0.0)
    }

    @Test
    fun testGetMonthsBetween_EndBeforeStart() {
        val start = LocalDate.of(2023, 1, 15)
        val end = LocalDate.of(2023, 1, 1)
        assertEquals(0.0, getMonthsBetween(start, end), 0.0)
    }

    @Test
    fun testAllocatePayment_InterestFirst() {
        // Outstanding interest is 100
        val outstanding = 100.0
        
        // Case 1: Payment covers only interest (50 < 100)
        val (i1, p1) = allocatePayment(50.0, outstanding)
        assertEquals(50.0, i1, 0.0)
        assertEquals(0.0, p1, 0.0)
        
        // Case 2: Payment covers exactly interest (100 = 100)
        val (i2, p2) = allocatePayment(100.0, outstanding)
        assertEquals(100.0, i2, 0.0)
        assertEquals(0.0, p2, 0.0)
        
        // Case 3: Payment covers interest + principal (150 > 100)
        val (i3, p3) = allocatePayment(150.0, outstanding)
        assertEquals(100.0, i3, 0.0)
        assertEquals(50.0, p3, 0.0)
        
        // Case 4: outstandingInterest == 0.0 — all goes to principal
        val (i4, p4) = allocatePayment(50.0, 0.0)
        assertEquals(0.0, i4, 0.0)
        assertEquals(50.0, p4, 0.0)
    }
}
