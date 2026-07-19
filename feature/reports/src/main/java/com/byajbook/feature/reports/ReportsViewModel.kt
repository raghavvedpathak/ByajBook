package com.byajbook.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byajbook.calculations.getCustomerReport
import com.byajbook.calculations.getDashboard
import com.byajbook.calculations.getMonthlyInterest
import com.byajbook.calculations.getOverdue
import com.byajbook.domain.model.*
import com.byajbook.domain.repository.CustomerRepository
import com.byajbook.domain.repository.RecordRepository
import com.byajbook.domain.repository.SettingsRepository
import com.byajbook.domain.service.PdfService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val recordRepository: RecordRepository,
    private val settingsRepository: SettingsRepository,
    private val pdfService: PdfService
) : ViewModel() {

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _pdfUri = MutableStateFlow<String?>(null)
    val pdfUri: StateFlow<String?> = _pdfUri.asStateFlow()

    // 1. Overview & Customer Rollups
    val customerReports: StateFlow<List<CustomerReport>> = combine(
        recordRepository.getAllActiveRecords(),
        customerRepository.getAllCustomers()
    ) { records, customers ->
        getCustomerReport(records, customers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overviewStats: StateFlow<DashboardStats?> = recordRepository.getAllActiveRecords()
        .map { getDashboard(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 2. Monthly Earnings (Cash-Basis)
    val monthlyEarnings: StateFlow<List<MonthlyEarning>> = recordRepository.getAllActiveRecords()
        .map { getMonthlyInterest(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Overdue Records (Activity-Based)
    val overdueRecords: StateFlow<List<OverdueRecord>> = combine(
        recordRepository.getAllActiveRecords(),
        flow { emit(recordRepository.getActiveRecordLastActivityMap()) } // Static fetch for now, can be optimized
    ) { records, activityMap ->
        getOverdue(records, activityMap, LocalDate.now(), 30)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isFabVisible: StateFlow<Boolean> = combine(
        _activeTabIndex,
        _selectedCustomer
    ) { tabIndex, customer ->
        when (tabIndex) {
            0 -> true // Overview
            1 -> customer != null // Customer selection
            else -> false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onTabSelected(index: Int) {
        _activeTabIndex.value = index
    }

    fun onCustomerSelected(customer: Customer?) {
        _selectedCustomer.value = customer
    }

    fun onFabClicked() {
        viewModelScope.launch {
            val businessInfo = settingsRepository.getSettings().first()
            val customers = customerRepository.getAllCustomers().first()
            val records = recordRepository.getAllActiveRecordsOnce()

            when (_activeTabIndex.value) {
                0 -> {
                    _pdfUri.value = pdfService.generateAllCustomersReport(customers, records, businessInfo)
                }
                1 -> {
                    val customer = _selectedCustomer.value
                    if (customer != null) {
                        val customerRecords = records.filter { it.customerId == customer.id }
                        _pdfUri.value = pdfService.generateCustomerStatement(customer, customerRecords, businessInfo)
                    }
                }
            }
        }
    }

    fun onPdfConsumed() {
        _pdfUri.value = null
    }
}
