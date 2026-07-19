package com.byajbook.data.entity

import androidx.room.*
import com.byajbook.domain.model.RecordStatus
import com.byajbook.domain.model.RecordType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "customers",
    indices = [Index(value = ["displayId"], unique = true)]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val displayId: String,
    val name: String,
    val phone: String,
    val address: String,
    val createdAt: LocalDate
)

@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["transactionId"], unique = true)
    ]
)
data class RecordEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val customerId: String,
    val type: RecordType,
    val startDate: LocalDateTime, // [FIX-TIMESTAMP-RECORD-1]
    // DO NOT CHANGE TO INTEGER — switching to paise storage requires a Room schema migration; see §4.2 for full rationale.
    val principalAmount: Double,
    val interestRate: Double,
    val endDate: LocalDate?,
    val status: RecordStatus,
    val settledDate: LocalDate?,
    // DO NOT CHANGE TO INTEGER — switching to paise storage requires a Room schema migration; see §4.2 for full rationale.
    @ColumnInfo(name = "calculatedInterest") val calculatedInterest: Double?,
    val linkedRecordId: String?,
    val customerName: String?
)

@Entity(
    tableName = "ledger_items",
    foreignKeys = [
        ForeignKey(
            entity = RecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recordId"])]
)
data class LedgerItemEntity(
    @PrimaryKey val id: String,
    val recordId: String,
    val name: String,
    val itemCategory: String,
    val description: String?,
    val weight: Double,
    val purity: Double,
    val rate: Double,
    val itemValue: Double,
    val lendPercentage: Double,
    val lendableAmount: Double
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = RecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recordId"])]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val recordId: String,
    // DO NOT CHANGE TO INTEGER — switching to paise storage requires a Room schema migration; see §4.2 for full rationale.
    val amount: Double,
    val date: LocalDateTime, // [FIX-TIMESTAMP-PAYMENT-1]
    val notes: String,
    // DO NOT CHANGE TO INTEGER — switching to paise storage requires a Room schema migration; see §4.2 for full rationale.
    val interestPaid: Double,
    // DO NOT CHANGE TO INTEGER — switching to paise storage requires a Room schema migration; see §4.2 for full rationale.
    val principalPaid: Double
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val phone: String,
    val address: String,
    // DO NOT CHANGE TO INTEGER — switching to paise storage requires a Room schema migration; see §4.2 for full rationale.
    val defaultInterestRate: Double
)

@Entity(
    tableName = "item_rates",
    indices = [Index(value = ["itemCategory", "effectiveDate"], unique = true)]
)
data class ItemRateEntity(
    @PrimaryKey val id: String,
    val itemCategory: String,
    val ratePerUnit: Double,
    val effectiveDate: LocalDate,
    val updatedAt: LocalDateTime
)
