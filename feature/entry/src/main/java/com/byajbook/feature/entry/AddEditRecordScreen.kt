package com.byajbook.feature.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byajbook.domain.model.*
import com.byajbook.ui.DateInputField
import com.byajbook.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecordBottomSheet(
    recordId: String? = null,
    viewModel: AddEditRecordViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(recordId) {
        viewModel.init(recordId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Text("Error: ${state.message}", modifier = Modifier.padding(16.dp))
            }
            is UiState.Success -> {
                AddEditRecordForm(
                    viewModel = viewModel,
                    onSave = { viewModel.save { onDismiss() } }
                )
            }
        }
    }
}

@Composable
fun AddEditRecordForm(
    viewModel: AddEditRecordViewModel,
    onSave: () -> Unit
) {
    val type by viewModel.type.collectAsStateWithLifecycle()
    val customerId by viewModel.customerId.collectAsStateWithLifecycle()
    val principal by viewModel.principalAmount.collectAsStateWithLifecycle()
    val rate by viewModel.interestRate.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val noEndDate by viewModel.hasNoEndDate.collectAsStateWithLifecycle()
    val linkedId by viewModel.linkedRecordId.collectAsStateWithLifecycle()
    val linkedLabel by viewModel.linkedRecordLabel.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    
    val startDateError by viewModel.startDateError.collectAsStateWithLifecycle()
    val endDateError by viewModel.endDateError.collectAsStateWithLifecycle()
    
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val givenRecords by viewModel.availableGivenRecords.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (linkedId == null) "New Record" else "Edit Record",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            // Type Toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == RecordType.GIVEN,
                    onClick = { viewModel.onTypeChanged(RecordType.GIVEN) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("GIVEN")
                }
                SegmentedButton(
                    selected = type == RecordType.TAKEN,
                    onClick = { viewModel.onTypeChanged(RecordType.TAKEN) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("TAKEN")
                }
            }
        }

        if (type == RecordType.TAKEN) {
            item {
                LinkedRecordPicker(
                    selectedId = linkedId,
                    selectedLabel = linkedLabel,
                    availableRecords = givenRecords,
                    onSelected = viewModel::onLinkSelected
                )
            }
        }

        item {
            CustomerPicker(
                selectedId = customerId,
                customers = customers,
                onSelected = viewModel::onCustomerChanged
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = principal,
                    onValueChange = viewModel::onPrincipalChanged,
                    label = { Text("Principal Amount") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = viewModel::onInterestRateChanged,
                    label = { Text("Rate (%)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }

        item {
            DateInputField(
                value = startDate,
                onValueChange = viewModel::onStartDateChanged,
                label = "Start Date",
                isError = startDateError != null,
                errorMessage = startDateError ?: ""
            )
        }

        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = noEndDate, onCheckedChange = viewModel::onNoEndDateToggled)
                    Text("No end date (Open-ended)")
                }
                if (!noEndDate) {
                    DateInputField(
                        value = endDate,
                        onValueChange = viewModel::onEndDateChanged,
                        label = "End Date",
                        isError = endDateError != null,
                        errorMessage = endDateError ?: ""
                    )
                }
            }
        }

        item {
            Text("Collateral Items", style = MaterialTheme.typography.titleMedium)
        }

        items(items) { item ->
            LedgerItemRow(
                item = item,
                onUpdate = viewModel::updateItem,
                onRemove = { viewModel.removeItem(item.id) }
            )
        }

        item {
            TextButton(onClick = viewModel::addItem) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add Item")
            }
        }

        item {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Text("Save Record")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedRecordPicker(
    selectedId: String?,
    selectedLabel: String,
    availableRecords: List<LedgerRecord>,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Link to Given Record") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = { onSelected(null); expanded = false }
            )
            availableRecords.forEach { record ->
                DropdownMenuItem(
                    text = { Text(record.transactionId) },
                    onClick = { onSelected(record.id); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPicker(
    selectedId: String?,
    customers: List<Customer>,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = customers.find { it.id == selectedId }?.name ?: "Select Customer"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Customer") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            customers.forEach { customer ->
                DropdownMenuItem(
                    text = { Text(customer.name) },
                    onClick = { onSelected(customer.id); expanded = false }
                )
            }
        }
    }
}

@Composable
fun LedgerItemRow(
    item: LedgerItem,
    onUpdate: (LedgerItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    label = { Text("Item Name") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.weight.toString(),
                    onValueChange = { onUpdate(item.copy(weight = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Weight (g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = item.purity.toString(),
                    onValueChange = { onUpdate(item.copy(purity = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Purity (%)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
    }
}
