package com.byajbook.data.repository

import androidx.room.withTransaction
import com.byajbook.data.dao.ItemRateDao
import com.byajbook.data.entity.ItemRateEntity
import com.byajbook.domain.model.ItemRate
import com.byajbook.domain.repository.ItemRateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRateRepositoryImpl @Inject constructor(
    private val itemRateDao: ItemRateDao,
    private val database: com.byajbook.data.db.AppDatabase
) : ItemRateRepository {

    private val mutex = Mutex()
    private val singleThreadDispatcher = Dispatchers.IO.limitedParallelism(1)

    override fun getCurrentRate(category: String): Flow<ItemRate?> =
        itemRateDao.getCurrentRate(category).map { it?.toDomain() }

    override fun getCurrentRates(): Flow<List<ItemRate>> =
        itemRateDao.getAll().map { list -> 
            // Group by category and take latest effective date
            list.groupBy { it.itemCategory }
                .map { entry -> entry.value.maxBy { it.effectiveDate } }
                .map { it.toDomain() }
        }

    override fun getRatesForDate(date: String): Flow<List<ItemRate>> =
        itemRateDao.getRatesForDate(date).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertRate(rate: ItemRate) {
        mutex.withLock {
            withContext(singleThreadDispatcher) {
                database.withTransaction {
                    val existing = itemRateDao.getByCategoryAndDate(rate.itemCategory, rate.effectiveDate.toString())
                    if (existing != null) {
                        itemRateDao.update(existing.copy(
                            ratePerUnit = rate.ratePerUnit,
                            updatedAt = LocalDateTime.now()
                        ))
                    } else {
                        itemRateDao.insert(rate.toEntity())
                    }
                }
            }
        }
    }

    private fun ItemRateEntity.toDomain() = ItemRate(
        id = id,
        itemCategory = itemCategory,
        ratePerUnit = ratePerUnit,
        effectiveDate = effectiveDate,
        updatedAt = updatedAt
    )

    private fun ItemRate.toEntity() = ItemRateEntity(
        id = id,
        itemCategory = itemCategory,
        ratePerUnit = ratePerUnit,
        effectiveDate = effectiveDate,
        updatedAt = updatedAt
    )
}
