package com.byajbook.data.repository

import androidx.room.withTransaction
import com.byajbook.data.backup.BackupSerializer
import com.byajbook.data.backup.model.*
import com.byajbook.data.dao.CustomerDao
import com.byajbook.data.dao.LedgerItemDao
import com.byajbook.data.dao.PaymentDao
import com.byajbook.data.dao.RecordDao
import com.byajbook.data.db.AppDatabase
import com.byajbook.data.entity.*
import com.byajbook.domain.model.*
import com.byajbook.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val customerDao: CustomerDao,
    private val recordDao: RecordDao,
    private val itemDao: LedgerItemDao,
    private val paymentDao: PaymentDao
) : BackupRepository {

    private val mutex = Mutex()
    private val singleThreadDispatcher = Dispatchers.IO.limitedParallelism(1)

    override suspend fun exportBackup(): String {
        val customers = customerDao.getAll().first().map { it.toBackup() }
        val records = recordDao.getAllActiveOnce().map { entity ->
            val items = itemDao.getByRecordId(entity.id).map { it.toBackup() }
            val payments = paymentDao.getByRecordIdOnce(entity.id).map { it.toBackup() }
            entity.toBackup(items, payments)
        }

        val wrapper = BackupWrapper(
            version = BackupSerializer.BACKUP_VERSION,
            customers = customers,
            records = records
        )

        return BackupSerializer.json.encodeToString(wrapper)
    }

    override suspend fun importBackup(json: String): Result<Unit> = runCatching {
        val element = BackupSerializer.json.parseToJsonElement(json)
        
        val wrapper = when (element) {
            is JsonArray -> {
                // Legacy bare-array format [FIX-IMPORT-LEGACY-1]
                val records = BackupSerializer.legacyJson.decodeFromJsonElement<List<BackupRecord>>(element)
                BackupWrapper(version = "legacy", customers = emptyList(), records = records)
            }
            is JsonObject -> {
                val version = element["version"]?.jsonPrimitive?.content
                when (version) {
                    "1.1" -> {
                        val dto = BackupSerializer.json.decodeFromJsonElement<BackupWrapper>(element)
                        BackupSerializer.migrate_1_1_to_1_2(dto)
                    }
                    BackupSerializer.BACKUP_VERSION -> {
                        BackupSerializer.json.decodeFromJsonElement<BackupWrapper>(element)
                    }
                    else -> throw Exception("This backup was created by a newer version of ByajBook. Please update the app before importing.")
                }
            }
            else -> throw Exception("Invalid backup format")
        }

        performImport(wrapper)
    }

    private suspend fun performImport(wrapper: BackupWrapper) {
        mutex.withLock {
            withContext(singleThreadDispatcher) {
                database.withTransaction {
                    // 1. Process Customers
                    wrapper.customers.forEach { backupCust ->
                        customerDao.insert(backupCust.toEntity())
                    }

                    // 2. Process Records
                    wrapper.records.forEach { backupRec ->
                        val transactionId = if (backupRec.transactionId.isEmpty()) {
                            val maxSeq = recordDao.getMaxTransactionIdSequence() ?: 0
                            "TXN-%06d".format(maxSeq + 1)
                        } else {
                            backupRec.transactionId
                        }
                        
                        val recordEntity = backupRec.toEntity(transactionId)
                        recordDao.insert(recordEntity)
                        
                        itemDao.insertAll(backupRec.items.map { it.toEntity() })
                        backupRec.payments.forEach { paymentDao.insert(it.toEntity()) }
                    }
                }
            }
        }
    }

    // Mappers
    private fun CustomerEntity.toBackup() = BackupCustomer(id, displayId, name, phone, address, createdAt.toString())
    private fun RecordEntity.toBackup(items: List<BackupItem>, payments: List<BackupPayment>) = 
        BackupRecord(
            id = id,
            customerId = customerId,
            type = type.name,
            status = status.name,
            startDate = startDate.toString(),
            principalAmount = principalAmount,
            interestRate = interestRate,
            endDate = endDate?.toString() ?: "", // [FIX-BACKUPRECORD-ENDDATE-1] null -> ""
            transactionId = transactionId,
            linkedRecordId = linkedRecordId,
            calculatedInterest = calculatedInterest,
            customerName = customerName,
            items = items,
            payments = payments
        )
    private fun LedgerItemEntity.toBackup() = BackupItem(id, recordId, name, itemCategory, description, weight, purity, rate, itemValue, lendPercentage, lendableAmount)
    private fun PaymentEntity.toBackup() = BackupPayment(id, recordId, amount, date.toString(), notes, interestPaid, principalPaid)

    private fun BackupCustomer.toEntity() = CustomerEntity(id, displayId, name, phone, address, LocalDate.parse(createdAt))
    private fun BackupRecord.toEntity(finalTxnId: String) = RecordEntity(id, finalTxnId, customerId, RecordType.valueOf(type), LocalDateTime.parse(startDate), principalAmount, interestRate, endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }, RecordStatus.valueOf(status), null, calculatedInterest, linkedRecordId, customerName)
    private fun BackupItem.toEntity() = LedgerItemEntity(id, recordId, name, itemCategory, description, weight, purity, rate, itemValue, lendPercentage, lendableAmount)
    private fun BackupPayment.toEntity() = PaymentEntity(id, recordId, amount, LocalDateTime.parse(date), notes, interestPaid, principalPaid)
}
