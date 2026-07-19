package com.byajbook.domain.repository

import com.byajbook.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CustomerRepository {
    fun getAllCustomers(): Flow<List<Customer>>
    fun getCustomerById(id: String): Flow<Customer?>
    suspend fun insertCustomer(customer: Customer)
    suspend fun updateCustomer(customer: Customer)
    suspend fun deleteCustomer(id: String)
}

interface RecordRepository {
    fun getRecordsByCustomer(customerId: String): Flow<List<LedgerRecord>>
    fun getActiveGivenRecords(): Flow<List<LedgerRecord>>
    fun getAllActiveRecords(): Flow<List<LedgerRecord>>
    suspend fun getAllActiveRecordsOnce(): List<LedgerRecord>
    suspend fun getRecordById(id: String): LedgerRecord?
    suspend fun insertRecord(record: LedgerRecord)
    suspend fun updateRecord(record: LedgerRecord)
    suspend fun deleteRecord(id: String)
    suspend fun forceDeleteRecord(id: String)
    suspend fun settleRecord(id: String, calculatedInterest: Double)
    suspend fun addPayment(payment: Payment)
    fun getTotalPaidFlow(): Flow<List<RecordPaymentTotal>>
    suspend fun getActiveRecordLastActivityMap(): Map<String, LocalDate?>
    suspend fun getTotalPaidByRecordIds(recordIds: List<String>): List<RecordPaymentTotal>
}

interface ItemRateRepository {
    fun getCurrentRate(category: String): Flow<ItemRate?>
    fun getCurrentRates(): Flow<List<ItemRate>>
    fun getRatesForDate(date: String): Flow<List<ItemRate>>
    suspend fun upsertRate(rate: ItemRate)
}

interface SettingsRepository {
    fun getSettings(): Flow<Settings>
    suspend fun updateSettings(settings: Settings)
    
    // KNOWN DEBT: this method is on SettingsRepository for DI convenience only (SettingsViewModel
    // already injects it). It deletes customers (+ cascaded records/items/payments) and has no relationship to settings.
    // TODO v2: move to DataManagementRepository when a separate data-management screen is introduced.
    suspend fun clearTransactionData()
}

interface BackupRepository {
    suspend fun exportBackup(): String
    suspend fun importBackup(json: String): Result<Unit>
}
