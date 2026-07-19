package com.byajbook.data.backup

import com.byajbook.data.backup.model.BackupPayment
import com.byajbook.data.backup.model.BackupRecord
import com.byajbook.data.backup.model.BackupWrapper
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMigrationTest {

    @Test
    fun testMigrate_1_1_to_1_2_CoercesTimestamps() {
        val record = BackupRecord(
            id = "r1",
            customerId = "c1",
            type = "GIVEN",
            status = "ACTIVE",
            startDate = "2026-04-23", // Date only
            principalAmount = 100.0,
            interestRate = 2.0,
            items = emptyList(),
            payments = listOf(
                BackupPayment("p1", "r1", 50.0, "2026-04-25", "Note", 10.0, 40.0) // Date only
            )
        )
        
        val wrapper = BackupWrapper(version = "1.1", customers = emptyList(), records = listOf(record))
        val migrated = BackupSerializer.migrate_1_1_to_1_2(wrapper)
        
        assertEquals(BackupSerializer.BACKUP_VERSION, migrated.version)
        assertEquals("2026-04-23T00:00:00", migrated.records[0].startDate)
        assertEquals("2026-04-25T00:00:00", migrated.records[0].payments[0].date)
    }

    @Test
    fun testMigrate_1_1_to_1_2_PreservesExistingTimestamps() {
        val record = BackupRecord(
            id = "r1",
            customerId = "c1",
            type = "GIVEN",
            status = "ACTIVE",
            startDate = "2026-04-23T14:30:00", // Already has T
            principalAmount = 100.0,
            interestRate = 2.0,
            items = emptyList(),
            payments = emptyList()
        )
        
        val wrapper = BackupWrapper(version = "1.1", customers = emptyList(), records = listOf(record))
        val migrated = BackupSerializer.migrate_1_1_to_1_2(wrapper)
        
        assertEquals("2026-04-23T14:30:00", migrated.records[0].startDate)
    }
}
