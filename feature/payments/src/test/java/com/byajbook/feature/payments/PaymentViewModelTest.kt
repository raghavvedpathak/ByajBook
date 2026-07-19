package com.byajbook.feature.payments

import com.byajbook.domain.model.*
import com.byajbook.domain.repository.RecordRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    private val recordRepository = mockk<RecordRepository>(relaxed = true)
    private lateinit var viewModel: PaymentViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PaymentViewModel(recordRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test fresh fetch guard on recordPayment`() = runTest {
        // [FIX-DEV-FRESHFETCH-1]
        val recordId = "r1"
        val record = LedgerRecord(
            id = recordId,
            transactionId = "TXN-001",
            customerId = "c1",
            type = RecordType.GIVEN,
            status = RecordStatus.ACTIVE,
            startDate = LocalDateTime.now().minusMonths(1),
            endDate = null,
            principalAmount = 1000.0,
            interestRate = 2.0,
            settledDate = null,
            calculatedInterest = null,
            linkedRecordId = null,
            items = emptyList(),
            payments = emptyList()
        )
        
        coEvery { recordRepository.getRecordById(recordId) } returns record
        
        viewModel.init(recordId)
        viewModel.onAmountChanged("500")
        
        // This should trigger recordRepository.getRecordById again
        viewModel.recordPayment { }
        
        coVerify(exactly = 2) { recordRepository.getRecordById(recordId) }
    }
}
