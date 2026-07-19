package com.byajbook.feature.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byajbook.calculations.allocatePayment
import com.byajbook.calculations.calculateRecordFinancials
import com.byajbook.domain.model.*
import com.byajbook.domain.repository.RecordRepository
import com.byajbook.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<PaymentData>>(UiState.Loading)
    val uiState: StateFlow<UiState<PaymentData>> = _uiState.asStateFlow()

    private val _paymentAmount = MutableStateFlow("")
    val paymentAmount = _paymentAmount.asStateFlow()

    fun init(recordId: String) {
        viewModelScope.launch {
            val record = recordRepository.getRecordById(recordId)
            if (record != null) {
                val financials = calculateRecordFinancials(record, LocalDate.now())
                _uiState.value = UiState.Success(PaymentData(record, financials))
            } else {
                _uiState.value = UiState.Error("Record not found")
            }
        }
    }

    fun onAmountChanged(amount: String) {
        _paymentAmount.value = amount
    }

    /**
     * [FIX-DEV-FRESHFETCH-1] Stale record guard.
     */
    fun recordPayment(onSuccess: () -> Unit) {
        val amount = _paymentAmount.value.toDoubleOrNull() ?: return
        val currentData = (uiState.value as? UiState.Success)?.data ?: return
        val recordId = currentData.record.id

        viewModelScope.launch {
            // Fresh fetch immediately before calculation
            val freshRecord = recordRepository.getRecordById(recordId) ?: return@launch
            
            val outstanding = calculateRecordFinancials(freshRecord, LocalDate.now()).outstandingInterest
            val (interestPaid, principalPaid) = allocatePayment(amount, outstanding)
            
            val payment = Payment(
                id = UUID.randomUUID().toString(),
                recordId = recordId,
                amount = amount,
                date = LocalDateTime.now(),
                notes = "Regular Payment",
                interestPaid = interestPaid,
                principalPaid = principalPaid
            )
            
            recordRepository.addPayment(payment)
            onSuccess()
        }
    }

    fun markAsSettled(onSuccess: () -> Unit) {
        val currentData = (uiState.value as? UiState.Success)?.data ?: return
        val recordId = currentData.record.id
        
        viewModelScope.launch {
            val freshRecord = recordRepository.getRecordById(recordId) ?: return@launch
            val totalInterest = calculateRecordFinancials(freshRecord, LocalDate.now()).totalInterest
            recordRepository.settleRecord(recordId, totalInterest)
            onSuccess()
        }
    }

    data class PaymentData(
        val record: LedgerRecord,
        val financials: Financials
    )
}
