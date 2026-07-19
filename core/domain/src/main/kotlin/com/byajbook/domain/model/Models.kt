package com.byajbook.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

enum class RecordType { GIVEN, TAKEN }
enum class RecordStatus { ACTIVE, SETTLED }

data class LedgerRecord(
    val id: String,
    val transactionId: String,
    val customerId: String,
    val type: RecordType,
    val status: RecordStatus,
    val startDate: LocalDateTime,
    val endDate: LocalDate?,
    val principalAmount: Double,
    val interestRate: Double,
    val settledDate: LocalDate?,
    val calculatedInterest: Double?,
    val linkedRecordId: String?,
    val items: List<LedgerItem>,
    val payments: List<Payment>
)

data class LedgerItem(
    val id: String,
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

data class Payment(
    val id: String,
    val recordId: String,
    val amount: Double,
    val date: LocalDateTime,
    val notes: String,
    val interestPaid: Double,
    val principalPaid: Double
)

data class Customer(
    val id: String,
    val displayId: String,
    val name: String,
    val phone: String,
    val address: String,
    val createdAt: LocalDate
)

data class ItemRate(
    val id: String,
    val itemCategory: String,
    val ratePerUnit: Double,
    val effectiveDate: LocalDate,
    val updatedAt: LocalDateTime
)

data class DashboardStats(
    val totalPrincipalGiven: Double,
    val totalInterestAccruedGiven: Double,
    val totalDueGiven: Double,
    val totalPrincipalTaken: Double,
    val totalInterestAccruedTaken: Double,
    val totalDueTaken: Double
)

data class DashboardData(
    val givenRecords: List<LedgerRecord>,
    val takenRecords: List<LedgerRecord>,
    val stats: DashboardStats
)

data class OverdueRecord(
    val record: LedgerRecord,
    val daysSinceActivity: Long,
    val lastActivityDate: LocalDate
)

data class CustomerReport(
    val customer: Customer,
    val activeRecordCount: Int,
    val totalPrincipal: Double,
    val totalInterestAccrued: Double,
    val totalDue: Double
)

data class MonthlyEarning(
    val month: YearMonth,
    val interestReceived: Double
)

data class Settings(
    val name: String,
    val phone: String,
    val address: String,
    val defaultInterestRate: Double
)

/**
 * [FIX-DELETESTATE-1] DeleteConfirmationState must be defined in :core:domain
 * (it is a business-layer signal, not a UI detail).
 */
data class DeleteConfirmationState(val recordId: String, val linkedCount: Int)

// DOMAIN TYPE — defined in :core:domain. Used by RecordRepository interface and DashboardViewModel.
// Mapped from: RecordTotalPaid (DAO projection in :core:data).
data class RecordPaymentTotal(
    val recordId: String,
    val totalPaid: Double
)

/**
 * Profit labels for customer history drill-down.
 */
sealed class ProfitState {
    data class InterimProfit(val amount: Double) : ProfitState()
    data class NetProfit(val amount: Double) : ProfitState()
    object NoProfit : ProfitState()
}

data class Financials(
    val totalInterest: Double,
    val totalPaid: Double,
    val interestPaid: Double,
    val principalPaid: Double,
    val outstandingInterest: Double,
    val outstandingPrincipal: Double,
    val totalDue: Double
)
