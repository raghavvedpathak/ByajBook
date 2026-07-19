package com.byajbook.data.dao

import androidx.room.*
import com.byajbook.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getById(id: String): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getByIdOnce(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(customer: CustomerEntity)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT MAX(CAST(SUBSTR(displayId, 6) AS INTEGER)) FROM customers")
    suspend fun getMaxDisplayIdSequence(): Int?

    @Query("DELETE FROM customers")
    suspend fun deleteAll()

    @Transaction
    suspend fun insertWithGeneratedId(customer: CustomerEntity) {
        val maxSeq = getMaxDisplayIdSequence() ?: 0
        val finalDisplayId = if (customer.displayId.isEmpty()) {
            "CUST-%04d".format(maxSeq + 1)
        } else {
            customer.displayId
        }
        insert(customer.copy(displayId = finalDisplayId))
    }
}

@Dao
interface RecordDao {
    @Query("""
        SELECT r.*, COALESCE(c.name, r.customerName) AS customerName 
        FROM records r 
        LEFT JOIN customers c ON r.customerId = c.id 
        WHERE r.status = :status 
        ORDER BY r.startDate DESC
    """)
    fun getByStatus(status: String): Flow<List<RecordEntity>>

    @Query("""
        SELECT r.*, COALESCE(c.name, r.customerName) AS customerName 
        FROM records r 
        LEFT JOIN customers c ON r.customerId = c.id 
        WHERE r.customerId = :customerId
    """)
    fun getByCustomer(customerId: String): Flow<List<RecordEntity>>

    @Query("""
        SELECT r.*, COALESCE(c.name, r.customerName) AS customerName 
        FROM records r 
        LEFT JOIN customers c ON r.customerId = c.id 
        WHERE r.status = 'ACTIVE' AND r.type = 'GIVEN'
    """)
    fun getActiveGivenRecords(): Flow<List<RecordEntity>>

    @Query("""
        SELECT r.*, COALESCE(c.name, r.customerName) AS customerName 
        FROM records r 
        LEFT JOIN customers c ON r.customerId = c.id 
        WHERE r.status = 'ACTIVE'
    """)
    fun getAllActive(): Flow<List<RecordEntity>>

    @Query("""
        SELECT r.*, COALESCE(c.name, r.customerName) AS customerName 
        FROM records r 
        LEFT JOIN customers c ON r.customerId = c.id 
        WHERE r.status = 'ACTIVE'
    """)
    suspend fun getAllActiveOnce(): List<RecordEntity>

    @Query("""
        SELECT r.*, COALESCE(c.name, r.customerName) AS customerName 
        FROM records r 
        LEFT JOIN customers c ON r.customerId = c.id 
        WHERE r.id = :id
    """)
    suspend fun getByIdOnce(id: String): RecordEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: RecordEntity)

    @Update
    suspend fun update(record: RecordEntity)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT MAX(CAST(SUBSTR(transactionId, 5) AS INTEGER)) FROM records")
    suspend fun getMaxTransactionIdSequence(): Int?

    @Query("SELECT COUNT(*) FROM records WHERE linkedRecordId = :id")
    suspend fun getLinkedCount(id: String): Int

    @Transaction
    suspend fun deleteRecordWithLinkCheck(id: String) {
        val linkedCount = getLinkedCount(id)
        if (linkedCount > 0) {
            throw com.byajbook.domain.exception.RecordLinkedTakenException(linkedCount)
        }
        deleteById(id)
    }
}

@Dao
interface LedgerItemDao {
    @Query("SELECT * FROM ledger_items WHERE recordId = :recordId")
    suspend fun getByRecordId(recordId: String): List<LedgerItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<LedgerItemEntity>)

    @Query("DELETE FROM ledger_items WHERE recordId = :recordId")
    suspend fun deleteByRecordId(recordId: String)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE recordId = :recordId ORDER BY date ASC")
    fun getByRecordId(recordId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE recordId = :recordId ORDER BY date ASC")
    suspend fun getByRecordIdOnce(recordId: String): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE recordId = :recordId")
    suspend fun deleteByRecordId(recordId: String)

    @Query("SELECT recordId, SUM(principalPaid + interestPaid) AS totalPaid FROM payments GROUP BY recordId")
    fun getTotalPaidFlow(): Flow<List<RecordTotalPaid>>

    @Query("SELECT recordId, SUM(amount) as totalPaid FROM payments WHERE recordId IN (:recordIds) GROUP BY recordId")
    suspend fun getTotalPaidByRecordIds(recordIds: List<String>): List<RecordTotalPaid>

    @Query("SELECT r.id as recordId, MAX(p.date) as lastPaymentDate FROM records r LEFT JOIN payments p ON p.recordId = r.id WHERE r.status = 'ACTIVE' GROUP BY r.id")
    suspend fun getActiveRecordLastActivityDates(): List<RecordActivityRow>
}

// DAO PROJECTION — internal to :core:data. Never expose outside this module.
// Maps to: RecordPaymentTotal (domain type in :core:domain).
data class RecordTotalPaid(
    val recordId: String,
    val totalPaid: Double
)

data class RecordActivityRow(
    val recordId: String,
    val lastPaymentDate: String?
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)
}

@Dao
interface ItemRateDao {
    @Query("SELECT * FROM item_rates ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<ItemRateEntity>>

    @Query("SELECT * FROM item_rates WHERE itemCategory = :category ORDER BY effectiveDate DESC LIMIT 1")
    fun getCurrentRate(category: String): Flow<ItemRateEntity?>

    @Query("SELECT * FROM item_rates GROUP BY itemCategory HAVING MAX(effectiveDate)")
    fun getCurrentRates(): Flow<List<ItemRateEntity>>

    @Query("SELECT * FROM item_rates WHERE effectiveDate = :date")
    fun getRatesForDate(date: String): Flow<List<ItemRateEntity>>

    @Query("SELECT * FROM item_rates WHERE itemCategory = :category AND effectiveDate = :date")
    suspend fun getByCategoryAndDate(category: String, date: String): ItemRateEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rate: ItemRateEntity)

    @Update
    suspend fun update(rate: ItemRateEntity)
}
