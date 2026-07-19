package com.byajbook.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.byajbook.data.converter.AppTypeConverters
import com.byajbook.data.dao.*
import com.byajbook.data.entity.*

@Database(
    entities = [
        CustomerEntity::class,
        RecordEntity::class,
        LedgerItemEntity::class,
        PaymentEntity::class,
        SettingsEntity::class,
        ItemRateEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun recordDao(): RecordDao
    abstract fun ledgerItemDao(): LedgerItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun settingsDao(): SettingsDao
    abstract fun itemRateDao(): ItemRateDao
}
