package com.byajbook.data.di

import android.content.Context
import androidx.room.Room
import com.byajbook.data.db.AppDatabase
import com.byajbook.data.db.migration.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "byajbook.db"
        ).addMigrations(*Migrations.ALL).build()

    @Provides
    fun provideCustomerDao(db: AppDatabase) = db.customerDao()

    @Provides
    fun provideRecordDao(db: AppDatabase) = db.recordDao()

    @Provides
    fun provideLedgerItemDao(db: AppDatabase) = db.ledgerItemDao()

    @Provides
    fun providePaymentDao(db: AppDatabase) = db.paymentDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase) = db.settingsDao()

    @Provides
    fun provideItemRateDao(db: AppDatabase) = db.itemRateDao()
}
