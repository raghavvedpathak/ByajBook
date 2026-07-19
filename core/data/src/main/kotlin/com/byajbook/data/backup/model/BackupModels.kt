package com.byajbook.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupWrapper(
    val version: String,
    val customers: List<BackupCustomer>,
    val records: List<BackupRecord>
)

@Serializable
data class BackupCustomer(
    val id: String,
    val displayId: String,
    val name: String,
    val phone: String,
    val address: String,
    val createdAt: String
)

@Serializable
data class BackupRecord(
    val id: String,
    val customerId: String,
    val type: String,
    val status: String,
    val startDate: String,
    val principalAmount: Double,
    val interestRate: Double,
    val endDate: String? = null, // [FIX-BACKUPRECORD-ENDDATE-1]
    val transactionId: String = "", // [FIX-BACKUPRECORD-DEFAULTS-1]
    val linkedRecordId: String? = null,
    val calculatedInterest: Double? = null,
    val customerName: String? = null, // [FIX-BACKUPRECORD-CUSTOMERNAME-1]
    val items: List<BackupItem>,
    val payments: List<BackupPayment>
)

@Serializable
data class BackupItem(
    val id: String,
    val recordId: String,
    val name: String,
    val itemCategory: String = "Unknown", // [FIX-BACKUPRECORD-DEFAULTS-1]
    val description: String? = null,
    val weight: Double,
    val purity: Double,
    val rate: Double,
    val itemValue: Double,
    val lendPercentage: Double,
    val lendableAmount: Double
)

@Serializable
data class BackupPayment(
    val id: String,
    val recordId: String,
    val amount: Double,
    val date: String,
    val notes: String,
    val interestPaid: Double,
    val principalPaid: Double
)
