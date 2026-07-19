package com.byajbook.data.repository

import androidx.room.withTransaction
import com.byajbook.data.dao.CustomerDao
import com.byajbook.data.entity.CustomerEntity
import com.byajbook.domain.model.Customer
import com.byajbook.domain.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val database: com.byajbook.data.db.AppDatabase
) : CustomerRepository {

    private val mutex = Mutex()
    private val singleThreadDispatcher = Dispatchers.IO.limitedParallelism(1)

    override fun getAllCustomers(): Flow<List<Customer>> =
        customerDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getCustomerById(id: String): Flow<Customer?> =
        customerDao.getById(id).map { it?.toDomain() }

    override suspend fun insertCustomer(customer: Customer) {
        mutex.withLock {
            withContext(singleThreadDispatcher) {
                database.withTransaction {
                    val displayId = if (customer.displayId.isEmpty()) {
                        val maxSeq = customerDao.getMaxDisplayIdSequence() ?: 0
                        "CUST-%04d".format(maxSeq + 1)
                    } else {
                        customer.displayId
                    }
                    customerDao.insert(customer.toEntity(displayId))
                }
            }
        }
    }

    override suspend fun updateCustomer(customer: Customer) {
        customerDao.update(customer.toEntity(customer.displayId))
    }

    override suspend fun deleteCustomer(id: String) {
        customerDao.deleteById(id)
    }

    private fun CustomerEntity.toDomain() = Customer(
        id = id,
        displayId = displayId,
        name = name,
        phone = phone,
        address = address,
        createdAt = createdAt
    )

    private fun Customer.toEntity(finalDisplayId: String) = CustomerEntity(
        id = id,
        displayId = finalDisplayId,
        name = name,
        phone = phone,
        address = address,
        createdAt = createdAt
    )
}
