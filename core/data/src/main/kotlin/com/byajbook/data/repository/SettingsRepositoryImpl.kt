package com.byajbook.data.repository

import androidx.room.withTransaction
import com.byajbook.data.dao.CustomerDao
import com.byajbook.data.dao.SettingsDao
import com.byajbook.data.db.AppDatabase
import com.byajbook.data.entity.SettingsEntity
import com.byajbook.domain.model.Settings
import com.byajbook.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val customerDao: CustomerDao,
    private val database: AppDatabase
) : SettingsRepository {

    override fun getSettings(): Flow<Settings> =
        settingsDao.getSettings().transform { entity ->
            if (entity == null) {
                val default = SettingsEntity(
                    id = 1,
                    name = "",
                    phone = "",
                    address = "",
                    defaultInterestRate = 2.0
                )
                settingsDao.upsert(default)
                emit(default.toDomain())
            } else {
                emit(entity.toDomain())
            }
        }

    override suspend fun updateSettings(settings: Settings) {
        settingsDao.upsert(settings.toEntity())
    }

    override suspend fun clearTransactionData() {
        database.withTransaction {
            customerDao.deleteAll()
        }
    }

    private fun SettingsEntity.toDomain() = Settings(
        name = name,
        phone = phone,
        address = address,
        defaultInterestRate = defaultInterestRate
    )

    private fun Settings.toEntity() = SettingsEntity(
        id = 1,
        name = name,
        phone = phone,
        address = address,
        defaultInterestRate = defaultInterestRate
    )
}
