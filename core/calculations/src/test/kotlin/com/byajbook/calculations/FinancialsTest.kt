package com.byajbook.calculations

import com.byajbook.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FinancialsTest {

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
    fun testCalculateRecordFinancials_Settled_FastPath() {
        val record = baseRecord.copy(
            status = RecordStatus.SETTLED,
            calculatedInterest = 200.0,
            settledDate = LocalDate.now()
        )
        val financials = calculateRecordFinancials(record, LocalDate.now())
        
        assertEquals(200.0, financials.totalInterest, 0.0)
        assertEquals(0.0, financials.totalDue, 0.0)
    }

    @Test
    fun testCalculateRecordFinancials_Settled_NullCalculatedInterest_NoSettledDate() {
        // Edge case: settled with null calculatedInterest AND null settledDate
        val record = baseRecord.copy(
            status = RecordStatus.SETTLED,
            calculatedInterest = null,
            settledDate = null
        )
        val financials = calculateRecordFinancials(record, LocalDate.now())
        
        assertEquals(0.0, financials.totalInterest, 0.0)
        assertEquals(1000.0, financials.totalDue, 0.0)
        assertEquals(0.0, financials.outstandingInterest, 0.0)
    }

    @Test
    fun testCalculateRecordFinancials_Active_NoPayments() {
        // 1000 at 2% for 1.5 months (Jan 31 -> Mar 5)
        val record = baseRecord.copy(
            startDate = LocalDateTime.of(2023, 1, 31, 10, 0),
            status = RecordStatus.ACTIVE
        )
        val targetDate = LocalDate.of(2023, 3, 5)
        val financials = calculateRecordFinancials(record, targetDate)

        // Interest = 1000 * 2 * 1.5 / 100 = 30.0
        assertEquals(30.0, financials.totalInterest, 0.0)
        assertEquals(1030.0, financials.totalDue, 0.0)
    }

    @Test
    fun testCalculateRecordFinancials_Active_WithEndDate_Capped() {
        // 1000 at 2% for 1.0 month (Jan 1 -> Feb 1)
        // Record has endDate Feb 1, but we calculate for Mar 1
        val record = baseRecord.copy(
            startDate = LocalDateTime.of(2023, 1, 1, 10, 0),
            endDate = LocalDate.of(2023, 2, 1),
            status = RecordStatus.ACTIVE
        )
        val targetDate = LocalDate.of(2023, 3, 1)
        val financials = calculateRecordFinancials(record, targetDate)

        // Interest should be capped at 1 month = 20.0
        assertEquals(20.0, financials.totalInterest, 0.0)
        assertEquals(1020.0, financials.totalDue, 0.0)
    }

    @Test
    fun testCalculateRecordFinancials_PartialPayments() {
        // 1000 at 2% for 1 month = 20 interest. Total due = 1020.
        // Payment of 15 (Interest-only part).
        val payment = Payment("p1", "1", 15.0, LocalDateTime.now(), "Note", 15.0, 0.0)
        val record = baseRecord.copy(
            startDate = LocalDateTime.now().minusMonths(1),
            status = RecordStatus.ACTIVE,
            payments = listOf(payment)
        )
        
        val financials = calculateRecordFinancials(record, LocalDate.now())
        
        assertEquals(20.0, financials.totalInterest, 1.0) // ~20
        assertEquals(15.0, financials.interestPaid, 0.0)
        assertEquals(5.0, financials.outstandingInterest, 1.0) // ~5
        assertEquals(1000.0, financials.outstandingPrincipal, 0.0)
    }

    @Test
    fun testCalculateRecordFinancials_Overpayments_FloorAtZero() {
        // Principal 1000. Large payment of 2000.
        val payment = Payment("p1", "1", 2000.0, LocalDateTime.now(), "Note", 50.0, 1950.0)
        val record = baseRecord.copy(
            status = RecordStatus.ACTIVE,
            payments = listOf(payment)
        )
        
        val financials = calculateRecordFinancials(record, LocalDate.now())
        
        // Balances should not be negative
        assertTrue(financials.outstandingInterest >= 0.0)
        assertTrue(financials.outstandingPrincipal >= 0.0)
        assertEquals(0.0, financials.totalDue, 0.0)
    }

    @Test
    fun testCalculateTotalItemValue_MultipleItems() {
        val item1 = LedgerItem("i1", "r1", "Gold", "G1", null, 10.0, 100.0, 1000.0, 10000.0, 80.0, 8000.0)
        val item2 = LedgerItem("i2", "r1", "Silver", "S1", null, 50.0, 100.0, 100.0, 5000.0, 70.0, 3500.0)
        
        val totalValue = calculateTotalItemValue(listOf(item1, item2))
        assertEquals(15000.0, totalValue, 0.0)
    }
}
