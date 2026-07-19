package com.byajbook.feature.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byajbook.domain.model.*
import com.byajbook.ui.UiState
import com.byajbook.ui.formatCurrency
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentModal(
    recordId: String,
    viewModel: PaymentViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val paymentAmount by viewModel.paymentAmount.collectAsStateWithLifecycle()

    LaunchedEffect(recordId) {
        viewModel.init(recordId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Payment") },
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
                PaymentContent(
                    data = data,
                    amountStr = paymentAmount,
                    onAmountChange = viewModel::onAmountChanged,
                    onSave = { viewModel.recordPayment(onSuccess) },
                    onSettle = { viewModel.markAsSettled(onSuccess) },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun PaymentContent(
    data: PaymentViewModel.PaymentData,
    amountStr: String,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onSettle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val interestPart = minOf(amount, data.financials.outstandingInterest)
    val principalPart = amount - interestPart

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RecordSummaryHeader(data.record, data.financials)
        }

        item {
            OutlinedTextField(
                value = amountStr,
                onValueChange = onAmountChange,
                label = { Text("Payment Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") }
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Allocation Preview", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Interest Paid")
                        Text(formatCurrency(interestPart), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Principal Reduction")
                        Text(formatCurrency(principalPart), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = amount > 0
            ) {
                Text("Confirm Payment")
            }
        }

        item {
            HorizontalDivider()
        }

        item {
            Text("Advanced Actions", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
        }

        item {
            OutlinedButton(
                onClick = onSettle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mark as Fully Settled")
            }
        }
        
        item {
            Text("Payment History", style = MaterialTheme.typography.titleMedium)
        }
        
        items(data.record.payments) { payment ->
            PaymentHistoryRow(payment)
        }
    }
}

@Composable
fun RecordSummaryHeader(record: LedgerRecord, financials: Financials) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm") }
    Card {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text(record.transactionId, style = MaterialTheme.typography.labelLarge)
            Text(record.startDate.format(formatter), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Principal: ${formatCurrency(record.principalAmount)}")
                Text("O/S Interest: ${formatCurrency(financials.outstandingInterest)}", color = Color(0xFFC62828))
            }
            Text("Total Due: ${formatCurrency(financials.totalDue)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PaymentHistoryRow(payment: Payment) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(formatCurrency(payment.amount), fontWeight = FontWeight.Bold)
            Text(payment.date.format(formatter), style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Int: ${formatCurrency(payment.interestPaid)}", style = MaterialTheme.typography.bodySmall)
            Text("Pri: ${formatCurrency(payment.principalPaid)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
