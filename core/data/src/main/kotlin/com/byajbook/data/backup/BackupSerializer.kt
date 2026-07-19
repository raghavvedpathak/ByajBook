package com.byajbook.data.backup

import com.byajbook.data.backup.model.BackupWrapper
import kotlinx.serialization.json.Json

object BackupSerializer {
    /**
     * Strict JSON instance for backup parsing.
     * ignoreUnknownKeys = false: a backup from a newer app version will throw
     * SerializationException, surfacing a clear "update your app" error.
     * isLenient = false: rejects malformed JSON (unquoted keys, trailing commas).
     */
    val json = Json { ignoreUnknownKeys = false; isLenient = false }

    /**
     * Lenient JSON instance for legacy bare-array format detection and parsing.
     */
    val legacyJson = Json { ignoreUnknownKeys = true; isLenient = true }

    const val BACKUP_VERSION = "1.2" // Bumped from 1.1: LocalDateTime change.

    /**
     * [FIX-BACKUP-MIGRATION-SKELETON-1] Active Migration 1.1 -> 1.2
     * Coerces date-only strings by appending T00:00:00.
     */
    fun migrate_1_1_to_1_2(wrapper: BackupWrapper): BackupWrapper {
        return wrapper.copy(
            version = BACKUP_VERSION,
            records = wrapper.records.map { record ->
                record.copy(
                    startDate = if (record.startDate.contains("T")) record.startDate else record.startDate + "T00:00:00",
                    payments = record.payments.map { payment ->
                        payment.copy(
                            date = if (payment.date.contains("T")) payment.date else payment.date + "T00:00:00"
                        )
                    }
                )
            }
        )
    }
}
