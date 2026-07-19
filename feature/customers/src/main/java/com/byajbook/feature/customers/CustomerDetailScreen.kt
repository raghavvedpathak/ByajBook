package com.byajbook.feature.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byajbook.domain.model.*
import com.byajbook.ui.UiState
import com.byajbook.ui.formatCurrency
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: String,
    viewModel: CustomerDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onRecordClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(customerId) {
        viewModel.init(customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Ledger") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Text("Error: ${state.message}", modifier = Modifier.padding(padding))
            }
            is UiState.Success -> {
                val data = state.data
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        CustomerInfoCard(data.customer)
                    }
                    
                    items(data.history) { item ->
                        LedgerRecordCard(
                            item = item,
                            onClick = { onRecordClick(item.record.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerInfoCard(customer: Customer) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(customer.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(customer.displayId, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("Phone: ${customer.phone}")
            Text("Address: ${customer.address}")
        }
    }
}

@Composable
fun LedgerRecordCard(
    item: CustomerDetailViewModel.RecordWithFinancials,
    onClick: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm") }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.record.transactionId, fontWeight = FontWeight.Bold)
                Text(
                    item.record.type.name,
                    color = if (item.record.type == RecordType.GIVEN) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
            
            // [FIX-TIMESTAMPCUSTOMERHISTORY-1] Full date AND time
            Text(item.record.startDate.format(formatter), style = MaterialTheme.typography.bodySmall)
            
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Principal: ${formatCurrency(item.record.principalAmount)}")
                Text("Due: ${formatCurrency(item.financials.totalDue)}", fontWeight = FontWeight.Bold)
            }
            
            if (item.profitState !is ProfitState.NoProfit) {
                Spacer(Modifier.height(4.dp))
                ProfitLine(item.profitState)
            }
        }
    }
}

@Composable
fun ProfitLine(state: ProfitState) {
    val (label, amount) = when (state) {
        is ProfitState.InterimProfit -> "Interim Profit" to state.amount
        is ProfitState.NetProfit -> "Net Profit" to state.amount
        else -> "" to 0.0
    }
    
    if (label.isNotEmpty()) {
        Text(
            text = "$label: ${formatCurrency(amount)}",
            color = Color(0xFF1976D2),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
