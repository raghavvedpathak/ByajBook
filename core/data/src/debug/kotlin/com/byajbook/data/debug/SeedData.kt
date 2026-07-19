package com.byajbook.data.debug

import com.byajbook.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * [PRE-BUILD-ACTION-5] Canonical seed dataset.
 * Used for debug database seeding and unit test expectations.
 */
object SeedData {
    val customerA = Customer(
        id = "cust-uuid-001",
        displayId = "CUST-0001",
        name = "Priya Sharma",
        phone = "9876543210",
        address = "123 Main St, Mumbai",
        createdAt = LocalDate.of(2025, 12, 1)
    )

    val customerB = Customer(
        id = "cust-uuid-002",
        displayId = "CUST-0002",
        name = "Ravi Patel",
        phone = "9123456780",
        address = "456 Park Ave, Delhi",
        createdAt = LocalDate.of(2025, 12, 15)
    )

    val customers = listOf(customerA, customerB)

    val goldItem = LedgerItem(
        id = "item-uuid-001",
        recordId = "rec-uuid-001",
        name = "Gold ring",
        itemCategory = "Gold 22K",
        description = "Initial collateral",
        weight = 10.0,
        purity = 91.6,
        rate = 6500.0,
        itemValue = 5954.0,
        lendPercentage = 80.0,
        lendableAmount = 4763.2
    )

    val record1 = LedgerRecord(
        id = "rec-uuid-001",
        transactionId = "TXN-000001",
        customerId = customerA.id,
        type = RecordType.GIVEN,
        status = RecordStatus.ACTIVE,
        startDate = LocalDateTime.of(2026, 1, 1, 10, 0, 0),
        endDate = null,
        principalAmount = 50000.0,
        interestRate = 2.0,
        settledDate = null,
        calculatedInterest = null,
        linkedRecordId = null,
        items = listOf(goldItem),
        payments = emptyList()
    )

    val paymentRec2 = Payment(
        id = "pay-uuid-001",
        recordId = "rec-uuid-002",
        amount = 1500.0,
        date = LocalDateTime.of(2026, 3, 15, 9, 0, 0),
        notes = "First payment",
        interestPaid = 675.0,
        principalPaid = 825.0
    )

    val record2 = LedgerRecord(
        id = "rec-uuid-002",
        transactionId = "TXN-000002",
        customerId = customerA.id,
        type = RecordType.GIVEN,
        status = RecordStatus.ACTIVE,
        startDate = LocalDateTime.of(2026, 2, 15, 14, 30, 0),
        endDate = null,
        principalAmount = 30000.0,
        interestRate = 1.5,
        settledDate = null,
        calculatedInterest = null,
        linkedRecordId = null,
        items = emptyList(),
        payments = listOf(paymentRec2)
    )

    val record3 = LedgerRecord(
        id = "rec-uuid-003",
        transactionId = "TXN-000003",
        customerId = customerB.id,
        type = RecordType.TAKEN,
        status = RecordStatus.ACTIVE,
        startDate = LocalDateTime.of(2026, 1, 15, 11, 0, 0),
        endDate = null,
        principalAmount = 20000.0,
        interestRate = 1.0,
        settledDate = null,
        calculatedInterest = null,
        linkedRecordId = record1.id,
        items = listOf(goldItem.copy(id = "item-uuid-002", recordId = "rec-uuid-003")),
        payments = emptyList()
    )

    val record4 = LedgerRecord(
        id = "rec-uuid-004",
        transactionId = "TXN-000004",
        customerId = customerB.id,
        type = RecordType.GIVEN,
        status = RecordStatus.ACTIVE,
        startDate = LocalDateTime.of(2025, 12, 1, 9, 0, 0),
        endDate = LocalDate.of(2026, 6, 1),
        principalAmount = 15000.0,
        interestRate = 2.5,
        settledDate = null,
        calculatedInterest = null,
        linkedRecordId = null,
        items = emptyList(),
        payments = emptyList()
    )

    val record5 = LedgerRecord(
        id = "rec-uuid-005",
        transactionId = "TXN-000005",
        customerId = customerA.id,
        type = RecordType.GIVEN,
        status = RecordStatus.SETTLED,
        startDate = LocalDateTime.of(2025, 10, 1, 8, 0, 0),
        endDate = LocalDate.of(2026, 1, 1),
        principalAmount = 10000.0,
        interestRate = 2.0,
        settledDate = LocalDate.of(2026, 1, 1),
        calculatedInterest = 600.0,
        linkedRecordId = null,
        items = emptyList(),
        payments = emptyList()
    )

    val records = listOf(record1, record2, record3, record4, record5)

    val rate1 = ItemRate(
        id = "rate-uuid-001",
        itemCategory = "Gold 22K",
        ratePerUnit = 6500.0,
        effectiveDate = LocalDate.of(2026, 1, 1),
        updatedAt = LocalDateTime.of(2026, 1, 1, 10, 0, 0)
    )

    val rate2 = ItemRate(
        id = "rate-uuid-002",
        itemCategory = "Silver",
        ratePerUnit = 85.0,
        effectiveDate = LocalDate.of(2026, 1, 1),
        updatedAt = LocalDateTime.of(2026, 1, 1, 10, 0, 0)
    )

    val itemRates = listOf(rate1, rate2)
}
