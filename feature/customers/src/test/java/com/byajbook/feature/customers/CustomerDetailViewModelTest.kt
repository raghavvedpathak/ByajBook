package com.byajbook.feature.customers

import com.byajbook.domain.model.*
import com.byajbook.domain.repository.CustomerRepository
import com.byajbook.domain.repository.RecordRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerDetailViewModelTest {

    private val customerRepository = mockk<CustomerRepository>(relaxed = true)
    private val recordRepository = mockk<RecordRepository>(relaxed = true)
    
    private lateinit var viewModel: CustomerDetailViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CustomerDetailViewModel(customerRepository, recordRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test ProfitState resolution for linked records`() = runTest {
        val customerId = "c1"
        val customer = Customer(customerId, "CUST-001", "John", "123", "Addr", LocalDate.now())
        
        val givenRecord = LedgerRecord(
            id = "r_given",
            transactionId = "TXN-G",
            customerId = customerId,
            type = RecordType.GIVEN,
            status = RecordStatus.ACTIVE,
            startDate = LocalDateTime.now().minusMonths(2),
            endDate = null,
            principalAmount = 1000.0,
            interestRate = 2.0,
            settledDate = null,
            calculatedInterest = null,
            linkedRecordId = null,
            items = emptyList(),
            payments = emptyList()
        )
        
        val takenRecord = LedgerRecord(
            id = "r_taken",
            transactionId = "TXN-T",
            customerId = customerId,
            type = RecordType.TAKEN,
            status = RecordStatus.ACTIVE,
            startDate = LocalDateTime.now().minusMonths(2),
            endDate = null,
            principalAmount = 1000.0,
            interestRate = 1.0,
            settledDate = null,
            calculatedInterest = null,
            linkedRecordId = "r_given",
            items = emptyList(),
            payments = emptyList()
        )
        
        every { customerRepository.getCustomerById(customerId) } returns flowOf(customer)
        every { recordRepository.getRecordsByCustomer(customerId) } returns flowOf(listOf(givenRecord, takenRecord))
        coEvery { recordRepository.getRecordById("r_given") } returns givenRecord
        
        viewModel.init(customerId)
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState is com.byajbook.ui.UiState.Success)
        val history = (uiState as com.byajbook.ui.UiState.Success).data.history
        
        val takenData = history.find { it.record.id == "r_taken" }
        // Given interest (2% * 2mo = 40) - Taken interest (1% * 2mo = 20) = 20 Profit
        assertTrue(takenData?.profitState is ProfitState.InterimProfit)
        assertEquals(20.0, (takenData?.profitState as ProfitState.InterimProfit).amount, 1.0)
    }
}
