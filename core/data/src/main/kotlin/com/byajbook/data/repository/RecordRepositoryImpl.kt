package com.byajbook.data.repository

import androidx.room.withTransaction
import com.byajbook.data.dao.LedgerItemDao
import com.byajbook.data.dao.PaymentDao
import com.byajbook.data.dao.RecordDao
import com.byajbook.data.entity.LedgerItemEntity
import com.byajbook.data.entity.PaymentEntity
import com.byajbook.data.entity.RecordEntity
import com.byajbook.domain.exception.RecordLinkedTakenException
import com.byajbook.domain.model.*
import com.byajbook.domain.repository.RecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordRepositoryImpl @Inject constructor(
    private val recordDao: RecordDao,
    private val ledgerItemDao: LedgerItemDao,
    private val paymentDao: PaymentDao,
    private val database: com.byajbook.data.db.AppDatabase
) : RecordRepository {

    private val mutex = Mutex()
    private val singleThreadDispatcher = Dispatchers.IO.limitedParallelism(1)

    override fun getRecordsByCustomer(customerId: String): Flow<List<LedgerRecord>> =
        recordDao.getByCustomer(customerId).map { entities ->
            entities.map { it.toSummaryRecord() }
        }

    override fun getActiveGivenRecords(): Flow<List<LedgerRecord>> =
        recordDao.getActiveGivenRecords().map { entities ->
            entities.map { it.toSummaryRecord() }
        }

    override fun getAllActiveRecords(): Flow<List<LedgerRecord>> =
        recordDao.getAllActive().map { entities ->
            entities.map { it.toSummaryRecord() }
        }

    override suspend fun getAllActiveRecordsOnce(): List<LedgerRecord> =
        recordDao.getAllActiveOnce().map { entity ->
            val items = ledgerItemDao.getByRecordId(entity.id)
            val payments = paymentDao.getByRecordIdOnce(entity.id)
            entity.toFullRecord(items, payments)
        }

    override suspend fun getRecordById(id: String): LedgerRecord? {
        val entity = recordDao.getByIdOnce(id) ?: return null
        val items = ledgerItemDao.getByRecordId(entity.id)
        val payments = paymentDao.getByRecordIdOnce(entity.id)
        return entity.toFullRecord(items, payments)
    }

    override suspend fun insertRecord(record: LedgerRecord) {
        mutex.withLock {
            withContext(singleThreadDispatcher) {
                database.withTransaction {
                    val transactionId = if (record.transactionId.isEmpty()) {
                        val maxSeq = recordDao.getMaxTransactionIdSequence() ?: 0
                        "TXN-%06d".format(maxSeq + 1)
                    } else {
                        record.transactionId
                    }
                    
                    val entity = record.toEntity(transactionId)
                    recordDao.insert(entity)
                    ledgerItemDao.insertAll(record.items.map { it.toEntity() })
                    record.payments.forEach { paymentDao.insert(it.toEntity()) }
                }
            }
        }
    }

    override suspend fun updateRecord(record: LedgerRecord) {
        database.withTransaction {
            recordDao.update(record.toEntity(record.transactionId))
            // Replacement strategy: delete existing items and insert current ones
            ledgerItemDao.deleteByRecordId(record.id)
            ledgerItemDao.insertAll(record.items.map { it.toEntity() })
        }
    }

    override suspend fun deleteRecord(id: String) {
        recordDao.deleteRecordWithLinkCheck(id)
    }

    override suspend fun forceDeleteRecord(id: String) {
        recordDao.deleteById(id)
    }

    override suspend fun settleRecord(id: String, calculatedInterest: Double) {
        database.withTransaction {
            val entity = recordDao.getByIdOnce(id) ?: return@withTransaction
            val updated = entity.copy(
                status = RecordStatus.SETTLED,
                settledDate = LocalDate.now(),
                calculatedInterest = calculatedInterest
            )
            recordDao.update(updated)
        }
    }

    override suspend fun addPayment(payment: Payment) {
        paymentDao.insert(payment.toEntity())
    }

    override fun getTotalPaidFlow(): Flow<List<RecordPaymentTotal>> =
        paymentDao.getTotalPaidFlow().map { list ->
            list.map { RecordPaymentTotal(it.recordId, it.totalPaid) }
        }

    override suspend fun getActiveRecordLastActivityMap(): Map<String, LocalDate?> {
        val rows = paymentDao.getLatestPaymentDates()
        val records = recordDao.getAllActiveOnce()
        
        val paymentMap = rows.associate { row ->
            val date = row.lastPaymentDate?.let {
                runCatching { LocalDateTime.parse(it) }.getOrNull()?.toLocalDate()
                    ?: runCatching { LocalDate.parse(it) }.getOrNull()
            }
            row.recordId to date
        }

        return records.associate { it.id to paymentMap[it.id] }
    }

    override suspend fun getTotalPaidByRecordIds(recordIds: List<String>): List<RecordPaymentTotal> {
        // Chunking handled by caller if needed, but adding a safe layer here
        return recordIds.chunked(500).flatMap { chunk ->
            paymentDao.getTotalPaidByRecordIds(chunk).map { 
                RecordPaymentTotal(it.recordId, it.totalPaid) 
            }
        }
    }

    private fun RecordEntity.toSummaryRecord() = LedgerRecord(
        id = id,
        transactionId = transactionId,
        customerId = customerId,
        type = type,
        status = status,
        startDate = startDate,
        endDate = endDate,
        principalAmount = principalAmount,
        interestRate = interestRate,
        settledDate = settledDate,
        calculatedInterest = calculatedInterest,
        linkedRecordId = linkedRecordId,
        items = emptyList(),
        payments = emptyList()
    )

    private fun RecordEntity.toFullRecord(
        items: List<LedgerItemEntity>,
        payments: List<PaymentEntity>
    ) = LedgerRecord(
        id = id,
        transactionId = transactionId,
        customerId = customerId,
        type = type,
        status = status,
        startDate = startDate,
        endDate = endDate,
        principalAmount = principalAmount,
        interestRate = interestRate,
        settledDate = settledDate,
        calculatedInterest = calculatedInterest,
        linkedRecordId = linkedRecordId,
        items = items.map { it.toDomain() },
        payments = payments.map { it.toDomain() }
    )

    private fun LedgerItemEntity.toDomain() = LedgerItem(
        id = id, recordId = recordId, name = name, itemCategory = itemCategory,
        description = description, weight = weight, purity = purity, rate = rate,
        itemValue = itemValue, lendPercentage = lendPercentage, lendableAmount = lendableAmount
    )

    private fun PaymentEntity.toDomain() = Payment(
        id = id, recordId = recordId, amount = amount, date = date,
        notes = notes, interestPaid = interestPaid, principalPaid = principalPaid
    )

    private fun LedgerRecord.toEntity(finalTransactionId: String) = RecordEntity(
        id = id, transactionId = finalTransactionId, customerId = customerId,
        type = type, startDate = startDate, principalAmount = principalAmount,
        interestRate = interestRate, endDate = endDate, status = status,
        settledDate = settledDate, calculatedInterest = calculatedInterest,
        linkedRecordId = linkedRecordId, customerName = null
    )

    private fun LedgerItem.toEntity() = LedgerItemEntity(
        id = id, recordId = recordId, name = name, itemCategory = itemCategory,
        description = description, weight = weight, purity = purity, rate = rate,
        itemValue = itemValue, lendPercentage = lendPercentage, lendableAmount = lendableAmount
    )

    private fun Payment.toEntity() = PaymentEntity(
        id = id, recordId = recordId, amount = amount, date = date,
        notes = notes, interestPaid = interestPaid, principalPaid = principalPaid
    )
}
