package com.byajbook.feature.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byajbook.domain.model.*
import com.byajbook.domain.repository.CustomerRepository
import com.byajbook.domain.repository.ItemRateRepository
import com.byajbook.domain.repository.RecordRepository
import com.byajbook.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddEditRecordViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val customerRepository: CustomerRepository,
    private val itemRateRepository: ItemRateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    // Form State
    private val _id = MutableStateFlow(UUID.randomUUID().toString())
    private val _transactionId = MutableStateFlow("")
    
    private val _type = MutableStateFlow(RecordType.GIVEN)
    val type = _type.asStateFlow()

    private val _customerId = MutableStateFlow<String?>(null)
    val customerId = _customerId.asStateFlow()

    private val _principalAmount = MutableStateFlow("")
    val principalAmount = _principalAmount.asStateFlow()

    private val _interestRate = MutableStateFlow("")
    val interestRate = _interestRate.asStateFlow()

    private val _startDate = MutableStateFlow("") // 8 digits raw
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow("") // 8 digits raw
    val endDate = _endDate.asStateFlow()

    private val _hasNoEndDate = MutableStateFlow(true)
    val hasNoEndDate = _hasNoEndDate.asStateFlow()

    private val _linkedRecordId = MutableStateFlow<String?>(null)
    val linkedRecordId = _linkedRecordId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkedRecordLabel: StateFlow<String> = _linkedRecordId
        .flatMapLatest { id ->
            if (id == null) flowOf("None")
            else flow {
                val record = recordRepository.getRecordById(id)
                emit(record?.transactionId ?: "None")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "None")

    private val _items = MutableStateFlow<List<LedgerItem>>(emptyList())
    val items = _items.asStateFlow()

    // Validation State
    private val _startDateError = MutableStateFlow<String?>(null)
    val startDateError = _startDateError.asStateFlow()

    private val _endDateError = MutableStateFlow<String?>(null)
    val endDateError = _endDateError.asStateFlow()

    // Support State
    val allCustomers = customerRepository.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableGivenRecords = recordRepository.getActiveGivenRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var isEditMode = false

    fun init(recordId: String?) {
        if (recordId == null) {
            isEditMode = false
            _uiState.value = UiState.Success(Unit)
            return
        }

        isEditMode = true
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val record = recordRepository.getRecordById(recordId)
            if (record != null) {
                _id.value = record.id
                _transactionId.value = record.transactionId
                _type.value = record.type
                _customerId.value = record.customerId
                _principalAmount.value = record.principalAmount.toString()
                _interestRate.value = record.interestRate.toString()
                _startDate.value = record.startDate.format(DateTimeFormatter.ofPattern("ddMMyyyy"))
                _endDate.value = record.endDate?.format(DateTimeFormatter.ofPattern("ddMMyyyy")) ?: ""
                _hasNoEndDate.value = record.endDate == null
                _linkedRecordId.value = record.linkedRecordId
                _items.value = record.items
                _uiState.value = UiState.Success(Unit)
            } else {
                _uiState.value = UiState.Error("Record not found")
            }
        }
    }

    fun onTypeChanged(newType: RecordType) {
        if (_type.value != newType) {
            _type.value = newType
            _linkedRecordId.value = null
        }
    }

    fun onCustomerChanged(id: String?) {
        _customerId.value = id
    }

    fun onPrincipalChanged(value: String) {
        _principalAmount.value = value
    }

    fun onInterestRateChanged(value: String) {
        _interestRate.value = value
    }

    fun onStartDateChanged(value: String) {
        _startDate.value = value
        validateDates()
    }

    fun onEndDateChanged(value: String) {
        _endDate.value = value
        validateDates()
    }

    fun onNoEndDateToggled(checked: Boolean) {
        _hasNoEndDate.value = checked
        if (checked) {
            _endDate.value = ""
            _endDateError.value = null
        } else {
            validateDates()
        }
    }

    private fun validateDates() {
        val start = parseRawDate(_startDate.value)
        val end = if (_hasNoEndDate.value) null else parseRawDate(_endDate.value)

        _startDateError.value = if (_startDate.value.length == 8 && start == null) "Invalid date" else null
        
        if (start != null && end != null && end.isBefore(start)) {
            _endDateError.value = "End date cannot be before start date"
        } else {
            _endDateError.value = if (_endDate.value.length == 8 && end == null) "Invalid date" else null
        }
    }

    /**
     * [FIX-LINK-CUSTOMER-AUTOFILL-1] & [FIX-LINKED-ITEM-AUTOFILL-1]
     */
    fun onLinkSelected(selectedId: String?) {
        _linkedRecordId.value = selectedId
        if (selectedId == null) {
            _items.value = emptyList() // Requirement: Clear items if link is cleared
            return
        }

        viewModelScope.launch {
            val givenRecord = recordRepository.getRecordById(selectedId)
            if (givenRecord != null) {
                // Auto-fill customer
                _customerId.value = givenRecord.customerId
                
                // Auto-copy items
                val copiedItems = givenRecord.items.map { item ->
                    item.copy(
                        id = UUID.randomUUID().toString(),
                        recordId = _id.value
                    )
                }
                _items.value = copiedItems
            }
        }
    }

    fun addItem() {
        val newItem = LedgerItem(
            id = UUID.randomUUID().toString(),
            recordId = _id.value,
            name = "",
            itemCategory = "Gold 22K",
            description = null,
            weight = 0.0,
            purity = 0.0,
            rate = 0.0,
            itemValue = 0.0,
            lendPercentage = 80.0,
            lendableAmount = 0.0
        )
        _items.value = _items.value + newItem
    }

    fun removeItem(itemId: String) {
        _items.value = _items.value.filter { it.id != itemId }
    }

    fun updateItem(updatedItem: LedgerItem) {
        _items.value = _items.value.map { if (it.id == updatedItem.id) updatedItem else it }
    }

    fun save(onSuccess: () -> Unit) {
        val principal = _principalAmount.value.toDoubleOrNull() ?: return
        val rate = _interestRate.value.toDoubleOrNull() ?: return
        
        val startLocalDate = parseRawDate(_startDate.value) ?: return
        val endLocalDate = if (_hasNoEndDate.value) null else parseRawDate(_endDate.value)

        val record = LedgerRecord(
            id = _id.value,
            transactionId = _transactionId.value,
            customerId = _customerId.value ?: return,
            type = _type.value,
            status = RecordStatus.ACTIVE,
            startDate = startLocalDate.atTime(LocalTime.now()),
            endDate = endLocalDate,
            principalAmount = principal,
            interestRate = rate,
            settledDate = null,
            calculatedInterest = null,
            linkedRecordId = _linkedRecordId.value,
            items = _items.value,
            payments = emptyList()
        )

        viewModelScope.launch {
            if (isEditMode) {
                recordRepository.updateRecord(record)
            } else {
                recordRepository.insertRecord(record)
            }
            onSuccess()
        }
    }

    private fun parseRawDate(raw: String): LocalDate? {
        if (raw.length != 8) return null
        return try {
            val formatter = DateTimeFormatter.ofPattern("ddMMyyyy")
            LocalDate.parse(raw, formatter)
        } catch (e: Exception) {
            null
        }
    }
}
