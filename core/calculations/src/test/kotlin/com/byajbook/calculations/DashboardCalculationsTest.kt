package com.byajbook.calculations

import com.byajbook.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class DashboardCalculationsTest {

    private val baseRecord = LedgerRecord(
        id = "1",
        transactionId = "TXN-000001",
        customerId = "CUST-0001",
        type = RecordType.GIVEN,
        status = RecordStatus.ACTIVE,
        startDate = LocalDateTime.now(),
        endDate = null,
        principalAmount = 1000.0,
        interestRate = 2.0,
        settledDate = null,
        calculatedInterest = null,
        linkedRecordId = null,
        items = emptyList(),
        payments = emptyList()
    )

    @Test
    fun testGetDashboard_AggregatesCorrectly() {
        val today = LocalDate.now()
        val record1 = baseRecord.copy(
            id = "r1",
            type = RecordType.GIVEN,
            principalAmount = 1000.0,
            interestRate = 2.0,
            startDate = LocalDateTime.now().minusMonths(1) // ~1 month interest (20.0)
        )
        val record2 = baseRecord.copy(
            id = "r2",
            type = RecordType.TAKEN,
            principalAmount = 500.0,
            interestRate = 1.0,
            startDate = LocalDateTime.now().minusMonths(1) // ~1 month interest (5.0)
        )

        val stats = getDashboard(listOf(record1, record2))

        assertEquals(1000.0, stats.totalPrincipalGiven, 0.0)
        assertEquals(20.0, stats.totalInterestAccruedGiven, 1.0) // small delta for rounding
        assertEquals(500.0, stats.totalPrincipalTaken, 0.0)
        assertEquals(5.0, stats.totalInterestAccruedTaken, 1.0)
    }

    @Test
    fun testGetMonthlyInterest_CashBasis() {
        val payment1 = Payment("p1", "r1", 100.0, LocalDateTime.of(2023, 1, 15, 10, 0), "Note", 30.0, 70.0)
        val payment2 = Payment("p2", "r1", 150.0, LocalDateTime.of(2023, 1, 20, 10, 0), "Note", 40.0, 110.0)
        val payment3 = Payment("p3", "r1", 200.0, LocalDateTime.of(2023, 2, 5, 10, 0), "Note", 50.0, 150.0)

        val record = baseRecord.copy(payments = listOf(payment1, payment2, payment3))
        
        val earnings = getMonthlyInterest(listOf(record))

        assertEquals(2, earnings.size)
        assertEquals(YearMonth.of(2023, 1), earnings[0].month)
        assertEquals(70.0, earnings[0].interestReceived, 0.0) // 30 + 40
        assertEquals(YearMonth.of(2023, 2), earnings[1].month)
        assertEquals(50.0, earnings[1].interestReceived, 0.0)
    }

    @Test
    fun testGetOverdue_FiltersCorrectly() {
        val today = LocalDate.of(2023, 5, 1)
        val record1 = baseRecord.copy(id = "r1", startDate = LocalDateTime.of(2023, 1, 1, 10, 0))
        val record2 = baseRecord.copy(id = "r2", startDate = LocalDateTime.of(2023, 4, 15, 10, 0))
        
        val latestPayments = mapOf(
            "r1" to LocalDate.of(2023, 2, 1), // > 30 days
            "r2" to LocalDate.of(2023, 4, 20)  // < 30 days
        )

        val overdue = getOverdue(listOf(record1, record2), latestPayments, today, 30)

        assertEquals(1, overdue.size)
        assertEquals("r1", overdue[0].record.id)
        assertEquals(89, overdue[0].daysSinceActivity) // Feb 1 to May 1
    }

    @Test
    fun testGetMonthlyInterest_NoPayments_ContributesZero() {
        // Record active for 6 months but zero payments
        val record = baseRecord.copy(
            startDate = LocalDateTime.now().minusMonths(6),
            payments = emptyList()
        )
        
        val earnings = getMonthlyInterest(listOf(record))
        assertTrue(earnings.isEmpty())
    }

    @Test
    fun testDashboardAndCustomerReportConsistency() {
        // [FIX-ARCH-PDFTEST-1] Consistency Assertion
        val customer = Customer("c1", "CUST-0001", "John", "123", "Addr", LocalDate.now())
        val record = baseRecord.copy(customerId = "c1")
        
        val dashboardStats = getDashboard(listOf(record))
        val customerReports = getCustomerReport(listOf(record), listOf(customer))
        
        val totalDueFromReports = customerReports.sumOf { it.totalDue }
        
        // Assert that the monetary totals match exactly between Dashboard and Reports
        assertEquals(dashboardStats.totalDueGiven + dashboardStats.totalDueTaken, totalDueFromReports, 0.001)
    }
}
