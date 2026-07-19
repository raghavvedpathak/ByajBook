package com.byajbook.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byajbook.domain.model.Customer
import com.byajbook.domain.repository.CustomerRepository
import com.byajbook.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<Customer>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Customer>>> = _uiState.asStateFlow()

    init {
        customerRepository.getAllCustomers()
            .combine(_searchQuery) { customers, query ->
                if (query.isBlank()) customers
                else customers.filter { it.name.contains(query, ignoreCase = true) || it.displayId.contains(query, ignoreCase = true) }
            }
            .onEach { _uiState.value = UiState.Success(it) }
            .catch { _uiState.value = UiState.Error(it.message ?: "Unknown Error") }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addCustomer(name: String, phone: String, address: String) {
        viewModelScope.launch {
            val customer = Customer(
                id = UUID.randomUUID().toString(),
                displayId = "", // Repository will generate CUST-XXXX
                name = name,
                phone = phone,
                address = address,
                createdAt = LocalDate.now()
            )
            customerRepository.insertCustomer(customer)
        }
    }
    
    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            customerRepository.deleteCustomer(id)
        }
    }
}
