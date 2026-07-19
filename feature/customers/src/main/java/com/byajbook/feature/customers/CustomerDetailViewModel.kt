package com.byajbook.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byajbook.domain.model.*
import com.byajbook.domain.repository.CustomerRepository
import com.byajbook.domain.repository.RecordRepository
import com.byajbook.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<CustomerDetailData>>(UiState.Loading)
    val uiState: StateFlow<UiState<CustomerDetailData>> = _uiState.asStateFlow()

    fun init(customerId: String) {
        customerRepository.getCustomerById(customerId)
            .flatMapLatest { customer ->
                if (customer == null) flowOf(UiState.Error("Customer not found"))
                else {
                    recordRepository.getRecordsByCustomer(customerId).map { records ->
                        val today = LocalDate.now()
                        val recordData = records.map { record ->
                            // Resolve profit state for TAKEN records
                            val profitState = if (record.type == RecordType.TAKEN && record.linkedRecordId != null) {
                                resolveProfitState(record, today)
                            } else {
                                ProfitState.NoProfit
                            }
                            
                            RecordWithFinancials(
                                record = record,
                                financials = com.byajbook.calculations.calculateRecordFinancials(record, today),
                                profitState = profitState
                            )
                        }
                        UiState.Success(CustomerDetailData(customer, recordData))
                    }
                }
            }
            .onEach { _uiState.value = it }
            .catch { e -> _uiState.value = UiState.Error(e.message ?: "Unknown Error") }
            .launchIn(viewModelScope)
    }

    private suspend fun resolveProfitState(takenRecord: LedgerRecord, today: LocalDate): ProfitState {
        val linkedId = takenRecord.linkedRecordId ?: return ProfitState.NoProfit
        val givenRecord = recordRepository.getRecordById(linkedId) ?: return ProfitState.NoProfit
        
        val givenFin = com.byajbook.calculations.calculateRecordFinancials(givenRecord, today)
        val takenFin = com.byajbook.calculations.calculateRecordFinancials(takenRecord, today)
        
        // INTENTIONAL: accrual-based profit (gross interest spread), not cash-adjusted for payments.
        // netProfit = givenFinancials.totalInterest - takenFinancials.totalInterest
        // Do NOT change to use outstandingInterest — that alters semantics. See spec §4.1 for rationale.
        val profitAmount = givenFin.totalInterest - takenFin.totalInterest
        
        return if (givenRecord.status == RecordStatus.ACTIVE) {
            ProfitState.InterimProfit(profitAmount)
        } else {
            ProfitState.NetProfit(profitAmount)
        }
    }

    data class CustomerDetailData(
        val customer: Customer,
        val history: List<RecordWithFinancials>
    )

    data class RecordWithFinancials(
        val record: LedgerRecord,
        val financials: Financials,
        val profitState: ProfitState
    )
}
