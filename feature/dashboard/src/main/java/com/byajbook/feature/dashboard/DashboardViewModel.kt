package com.byajbook.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byajbook.calculations.computeCollectionAlerts
import com.byajbook.calculations.getDashboard
import com.byajbook.domain.model.*
import com.byajbook.domain.repository.CustomerRepository
import com.byajbook.domain.repository.ItemRateRepository
import com.byajbook.domain.repository.RecordRepository
import com.byajbook.ui.model.RecordAlertGroup
import com.byajbook.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val itemRateRepository: ItemRateRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Given, 1: Taken
    val selectedTab = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardData>> = _uiState.asStateFlow()

    private val _rateErrors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val rateErrors = _rateErrors.asStateFlow()

    private val _addCategoryError = MutableStateFlow<String?>(null)
    val addCategoryError = _addCategoryError.asStateFlow()

    private val _isSafeRecordsExpanded = MutableStateFlow(false)
    val isSafeRecordsExpanded = _isSafeRecordsExpanded.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        combine(
            recordRepository.getAllActiveRecords(),
            _searchQuery,
            _selectedTab
        ) { records, query, tab ->
            val type = if (tab == 0) RecordType.GIVEN else RecordType.TAKEN
            val filtered = records.filter { it.type == type }
                .filter { it.transactionId.contains(query, ignoreCase = true) }
            
            val stats = getDashboard(records)
            val given = if (tab == 0) filtered else records.filter { it.type == RecordType.GIVEN }
            val taken = if (tab == 1) filtered else records.filter { it.type == RecordType.TAKEN }

            DashboardData(given, taken, stats)
        }
        .onEach { _uiState.value = UiState.Success(it) }
        .catch { e -> _uiState.value = UiState.Error(e.message ?: "Unknown Error") }
        .launchIn(viewModelScope)
    }

    /**
     * [FIX-DEV-DEBOUNCE-1] & [FIX-DEV-COMBINESUSPEND-1]
     */
    val collectionAlerts: StateFlow<List<CollectionAlert>> = combine(
        recordRepository.getActiveGivenRecords(),
        itemRateRepository.getCurrentRates().debounce(300.milliseconds),
        recordRepository.getTotalPaidFlow()
    ) { records, rates, totalPaidList ->
        val totalPaidMap = totalPaidList.associateBy({ it.recordId }, { it.totalPaid })
        computeCollectionAlerts(records, rates, totalPaidMap)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * [FIX-RATEMISSING-DISPLAY-1] Grouped alerts per record
     */
    val recordAlertGroups: StateFlow<List<RecordAlertGroup>> = combine(
        collectionAlerts,
        customerRepository.getAllCustomers(),
        recordRepository.getTotalPaidFlow()
    ) { alerts, customers, totalPaidList ->
        val customerMap = customers.associateBy { it.id }
        val totalPaidMap = totalPaidList.associateBy({ it.recordId }, { it.totalPaid })
        
        alerts.groupBy { alert ->
            when(alert) {
                is CollectionAlert.CollateralDrop -> alert.record.id
                is CollectionAlert.OvershootWarning -> alert.record.id
                is CollectionAlert.RateMissing -> alert.record.id
            }
        }.map { (_, group) -> 
            val record = when(val first = group.first()) {
                is CollectionAlert.CollateralDrop -> first.record
                is CollectionAlert.OvershootWarning -> first.record
                is CollectionAlert.RateMissing -> first.record
            }
            val name = customerMap[record.customerId]?.name ?: record.customerName
            
            RecordAlertGroup(
                record = record,
                alerts = group,
                totalPaid = totalPaidMap[record.id] ?: 0.0,
                customerName = name
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val safeRecords: StateFlow<List<LedgerRecord>> = combine(
        recordRepository.getActiveGivenRecords(),
        recordAlertGroups
    ) { records, atRiskGroups ->
        val atRiskIds = atRiskGroups.map { it.record.id }.toSet()
        records.filter { it.id !in atRiskIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class RiskSummary(val atRiskCount: Int, val totalExposure: Double)
    
    val riskSummary: StateFlow<RiskSummary> = recordAlertGroups.map { groups ->
        val today = LocalDate.now()
        val totalExposure = groups.sumOf { group ->
            // Use the authoritative calculation engine for consistency
            com.byajbook.calculations.calculateRecordFinancials(group.record, today).totalDue
        }
        RiskSummary(groups.size, totalExposure)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RiskSummary(0, 0.0))

    /**
     * H-4 FIX — alertsLoaded StateFlow
     */
    val alertsLoaded: StateFlow<Boolean> = collectionAlerts
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val staleRateDate: StateFlow<LocalDate?> = itemRateRepository.getCurrentRates().map { rates ->
        val today = LocalDate.now()
        rates.map { it.effectiveDate }.maxOrNull()?.takeIf { it.isBefore(today) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentRates: StateFlow<List<ItemRate>> = itemRateRepository.getCurrentRates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleSafeRecords() {
        _isSafeRecordsExpanded.value = !_isSafeRecordsExpanded.value
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
    }

    fun updateRate(category: String, rateStr: String) {
        val rate = rateStr.toDoubleOrNull()
        if (rate == null || rate <= 0) {
            _rateErrors.value = _rateErrors.value + (category to "Rate must be greater than zero")
            return
        }
        _rateErrors.value = _rateErrors.value + (category to null)
        viewModelScope.launch {
            itemRateRepository.upsertRate(
                ItemRate(
                    id = UUID.randomUUID().toString(),
                    itemCategory = category,
                    ratePerUnit = rate,
                    effectiveDate = LocalDate.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) {
            _addCategoryError.value = "Category name cannot be blank"
            return
        }
        viewModelScope.launch {
            val existing = itemRateRepository.getCurrentRates().first()
            if (existing.any { it.itemCategory.equals(name, ignoreCase = true) }) {
                _addCategoryError.value = "Category already exists"
                return@launch
            }
            _addCategoryError.value = null
            itemRateRepository.upsertRate(
                ItemRate(
                    id = UUID.randomUUID().toString(),
                    itemCategory = name,
                    ratePerUnit = 0.0,
                    effectiveDate = LocalDate.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        }
    }

    fun clearAddCategoryError() {
        _addCategoryError.value = null
    }
}
