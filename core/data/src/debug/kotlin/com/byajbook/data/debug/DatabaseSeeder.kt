package com.byajbook.data.debug

import com.byajbook.data.db.AppDatabase
import com.byajbook.data.entity.*
import com.byajbook.domain.model.*
import kotlinx.coroutines.flow.first

/**
 * [PRE-BUILD-ACTION-5] & [W2-DELIVERABLE]
 * Debug-only database seeder using the canonical SeedData dataset.
 */
suspend fun seedDatabase(db: AppDatabase) {
    // 1. Check if already seeded to avoid duplicates
    val existing = db.customerDao().getAll().first()
    if (existing.isNotEmpty()) return

    // 2. Seed Customers
    SeedData.customers.forEach { 
        db.customerDao().insert(it.toEntity())
    }

    // 3. Seed Item Rates
    SeedData.itemRates.forEach {
        db.itemRateDao().insert(it.toEntity())
    }

    // 4. Seed Records, Items and Payments
    SeedData.records.forEach { record ->
        db.recordDao().insert(record.toEntity())
        
        // Items
        record.items.forEach { item ->
            db.ledgerItemDao().insertAll(listOf(item.toEntity()))
        }

        // Payments
        record.payments.forEach { payment ->
            db.paymentDao().insert(payment.toEntity())
        }
    }
}

// Minimal mappers for seeding
private fun Customer.toEntity() = CustomerEntity(id, displayId, name, phone, address, createdAt)
private fun ItemRate.toEntity() = ItemRateEntity(id, itemCategory, ratePerUnit, effectiveDate, updatedAt)
private fun LedgerRecord.toEntity() = RecordEntity(id, transactionId, customerId, type, startDate, principalAmount, interestRate, endDate, status, settledDate, calculatedInterest, linkedRecordId, null)
private fun LedgerItem.toEntity() = LedgerItemEntity(id, recordId, name, itemCategory, description, weight, purity, rate, itemValue, lendPercentage, lendableAmount)
private fun Payment.toEntity() = PaymentEntity(id, recordId, amount, date, notes, interestPaid, principalPaid)
