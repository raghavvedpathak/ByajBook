package com.byajbook.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.byajbook.data.dao.CustomerDao
import com.byajbook.data.dao.RecordDao
import com.byajbook.data.entity.CustomerEntity
import com.byajbook.data.entity.RecordEntity
import com.byajbook.domain.model.RecordStatus
import com.byajbook.domain.model.RecordType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var db: AppDatabase
    private lateinit var customerDao: CustomerDao
    private lateinit var recordDao: RecordDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        customerDao = db.customerDao()
        recordDao = db.recordDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testCustomerInsertAndRead() = runBlocking {
        val customer = CustomerEntity("c1", "CUST-0001", "John", "123", "Addr", LocalDate.now())
        customerDao.insert(customer)
        val all = customerDao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("John", all[0].name)
    }

    @Test
    fun testCascadeDelete() = runBlocking {
        val customer = CustomerEntity("c1", "CUST-0001", "John", "123", "Addr", LocalDate.now())
        customerDao.insert(customer)
        
        val record = RecordEntity(
            id = "r1",
            transactionId = "TXN-000001",
            customerId = "c1",
            type = RecordType.GIVEN,
            status = RecordStatus.ACTIVE,
            startDate = LocalDateTime.now(),
            principalAmount = 1000.0,
            interestRate = 2.0,
            endDate = null,
            settledDate = null,
            calculatedInterest = null,
            linkedRecordId = null,
            customerName = null
        )
        recordDao.insert(record)
        
        // Verify insert
        assertEquals(1, recordDao.getAllActiveOnce().size)
        
        // Delete customer and verify cascade
        customerDao.deleteById("c1")
        assertEquals(0, recordDao.getAllActiveOnce().size)
    }

    @Test
    fun testLocalDateTimePreservation() = runBlocking {
        val start = LocalDateTime.of(2026, 4, 23, 14, 30, 0)
        val customer = CustomerEntity("c1", "CUST-0001", "John", "123", "Addr", LocalDate.now())
        customerDao.insert(customer)
        
        val record = RecordEntity(
            id = "r1",
            transactionId = "TXN-000001",
            customerId = "c1",
            type = RecordType.GIVEN,
            status = RecordStatus.ACTIVE,
            startDate = start,
            principalAmount = 1000.0,
            interestRate = 2.0,
            endDate = null,
            settledDate = null,
            calculatedInterest = null,
            linkedRecordId = null,
            customerName = null
        )
        recordDao.insert(record)
        
        val loaded = recordDao.getByIdOnce("r1")
        assertEquals(start, loaded?.startDate)
        assertEquals(14, loaded?.startDate?.hour)
        assertEquals(30, loaded?.startDate?.minute)
    }
}
