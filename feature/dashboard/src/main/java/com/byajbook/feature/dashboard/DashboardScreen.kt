package com.byajbook.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byajbook.domain.model.*
import com.byajbook.ui.model.RecordAlertGroup
import com.byajbook.ui.UiState
import com.byajbook.ui.formatCurrency
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onAddRecord: () -> Unit,
    onRecordClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val alerts by viewModel.recordAlertGroups.collectAsStateWithLifecycle()
    val alertsLoaded by viewModel.alertsLoaded.collectAsStateWithLifecycle()
    val staleDate by viewModel.staleRateDate.collectAsStateWithLifecycle()
    val rates by viewModel.currentRates.collectAsStateWithLifecycle()
    val rateErrors by viewModel.rateErrors.collectAsStateWithLifecycle()
    val addCategoryError by viewModel.addCategoryError.collectAsStateWithLifecycle()
    val riskSummary by viewModel.riskSummary.collectAsStateWithLifecycle()
    val safeRecords by viewModel.safeRecords.collectAsStateWithLifecycle()
    val isSafeExpanded by viewModel.isSafeRecordsExpanded.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = com.byajbook.ui.R.drawable.ic_byajbook_logo),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ByajBook",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRecord,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Entry") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WelcomeHeader(searchQuery, viewModel::onSearchQueryChanged)
            }

            item {
                SummaryCards(stats = (uiState as? UiState.Success)?.data?.stats, isGiven = selectedTab == 0)
            }
            
            item {
                RateManagementCard(
                    rates = rates,
                    rateErrors = rateErrors,
                    addCategoryError = addCategoryError,
                    onUpdateRate = viewModel::updateRate,
                    onAddCategory = viewModel::addCategory,
                    onClearAddCategoryError = viewModel::clearAddCategoryError
                )
            }

            item {
                RiskSection(
                    alerts = alerts,
                    summary = riskSummary,
                    loaded = alertsLoaded,
                    staleDate = staleDate,
                    rates = rates,
                    safeRecords = safeRecords,
                    isSafeExpanded = isSafeExpanded,
                    onToggleSafe = viewModel::toggleSafeRecords,
                    onRecordClick = onRecordClick
                )
            }

            item {
                DashboardTabs(
                    selectedTab = selectedTab,
                    onTabSelected = viewModel::onTabSelected
                )
            }

            when (val state = uiState) {
                is UiState.Loading -> {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                }
                is UiState.Error -> {
                    item { Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error) }
                }
                is UiState.Success -> {
                    val records = if (selectedTab == 0) state.data.givenRecords else state.data.takenRecords
                    
                    items(records) { record ->
                        RecordRow(record = record, onClick = { onRecordClick(record.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeHeader(query: String, onQueryChange: (String) -> Unit) {
    Column {
        Text(
            "WellCome",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Manage your ledger and risks here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search by Transaction ID") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = MaterialTheme.shapes.large,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
fun SummaryCards(stats: DashboardStats?, isGiven: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val principal = if (isGiven) stats?.totalPrincipalGiven ?: 0.0 else stats?.totalPrincipalTaken ?: 0.0
        val interest = if (isGiven) stats?.totalInterestAccruedGiven ?: 0.0 else stats?.totalInterestAccruedTaken ?: 0.0
        
        SummaryCard(
            label = "Principal",
            amount = principal,
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        SummaryCard(
            label = "Interest",
            amount = interest,
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun SummaryCard(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun RateManagementCard(
    rates: List<ItemRate>,
    rateErrors: Map<String, String?>,
    addCategoryError: String?,
    onUpdateRate: (String, String) -> Unit,
    onAddCategory: (String) -> Unit,
    onClearAddCategoryError: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Market Rates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            rates.forEach { rate ->
                RateRow(
                    rate = rate,
                    error = rateErrors[rate.itemCategory],
                    onUpdate = onUpdateRate
                )
            }
            
            var newCat by remember { mutableStateOf("") }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCat,
                        onValueChange = { 
                            newCat = it 
                            if (addCategoryError != null) onClearAddCategoryError()
                        },
                        label = { Text("New Category") },
                        modifier = Modifier.weight(1f),
                        isError = addCategoryError != null,
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { 
                            onAddCategory(newCat)
                            if (addCategoryError == null) {
                                newCat = ""
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
                if (addCategoryError != null) {
                    Text(
                        text = addCategoryError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RateRow(
    rate: ItemRate, 
    error: String?,
    onUpdate: (String, String) -> Unit
) {
    var textValue by remember { mutableStateOf(rate.ratePerUnit.toString()) }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rate.itemCategory, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier.width(110.dp),
                singleLine = true,
                isError = error != null,
                shape = MaterialTheme.shapes.medium,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            TextButton(onClick = { onUpdate(rate.itemCategory, textValue) }) {
                Text("Update", style = MaterialTheme.typography.labelMedium)
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun RiskSection(
    alerts: List<RecordAlertGroup>,
    summary: DashboardViewModel.RiskSummary,
    loaded: Boolean,
    staleDate: LocalDate?,
    rates: List<ItemRate>,
    safeRecords: List<LedgerRecord>,
    isSafeExpanded: Boolean,
    onToggleSafe: () -> Unit,
    onRecordClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (staleDate != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        "Market rates are stale (last updated ${staleDate}). Update now for accurate risk alerts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (!loaded) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else {
            RiskSummaryHeader(summary)
            
            alerts.forEach { group ->
                AlertCard(group = group, rates = rates, onClick = { onRecordClick(group.record.id) })
            }

            SafeRecordsSection(
                count = safeRecords.size,
                expanded = isSafeExpanded,
                onToggle = onToggleSafe,
                records = safeRecords,
                onRecordClick = onRecordClick
            )
        }
    }
}

@Composable
fun RiskSummaryHeader(summary: DashboardViewModel.RiskSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (summary.atRiskCount == 0) "All records safe" else "At Risk",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (summary.atRiskCount > 0) {
            Badge(containerColor = MaterialTheme.colorScheme.error) {
                Text("${summary.atRiskCount} Records", modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
    if (summary.atRiskCount > 0) {
        Text(
            text = "${formatCurrency(summary.totalExposure)} total exposure",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun AlertCard(group: RecordAlertGroup, rates: List<ItemRate>, onClick: () -> Unit) {
    val today = remember { LocalDate.now() }
    val financials = remember(group.record) { 
        com.byajbook.calculations.calculateRecordFinancials(group.record, today) 
    }
    val totalCurrentCollateralValue = remember(group.record, rates) {
        val rateMap = rates.associateBy { it.itemCategory }
        group.record.items.sumOf { item ->
            val rate = rateMap[item.itemCategory]?.ratePerUnit ?: 0.0
            item.weight * (item.purity / 100.0) * rate
        }
    }
    val projectedOutstanding = remember(group.record, group.totalPaid) {
        val projectedInterest = com.byajbook.calculations.calculateInterestForPeriod(
            principal = group.record.principalAmount,
            rate = group.record.interestRate,
            start = group.record.startDate.toLocalDate(),
            end = today.plusMonths(2)
        )
        group.record.principalAmount + projectedInterest - group.totalPaid
    }
    val itemValueAtLending = remember(group.record) {
        group.record.items.sumOf { it.itemValue }
    }
    val rateMissingAlerts = remember(group.alerts) {
        group.alerts.filterIsInstance<CollectionAlert.RateMissing>()
    }

    val isUnderwater = totalCurrentCollateralValue <= financials.totalDue
    val isOvershoot = projectedOutstanding >= itemValueAtLending
    val hasTriggered = isUnderwater || isOvershoot || rateMissingAlerts.isNotEmpty()

    val containerColor = if (hasTriggered) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }
    
    val borderColor = if (hasTriggered) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${group.customerName ?: "Unknown"} • ${group.record.transactionId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hasTriggered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (hasTriggered) {
                    Text(
                        text = "URGENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Collateral today: ${formatCurrency(totalCurrentCollateralValue)} → Due today: ${formatCurrency(financials.totalDue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (isUnderwater) Icons.Default.Warning else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isUnderwater) Color(0xFFFFB300) else Color(0xFF2E7D32),
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Due in 2 months: ${formatCurrency(projectedOutstanding)} → Item value at lending: ${formatCurrency(itemValueAtLending)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (isOvershoot) Icons.Default.Warning else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isOvershoot) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (rateMissingAlerts.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rateMissingAlerts.forEach { alert ->
                        SuggestionChip(
                            onClick = {}, 
                            label = { Text("Missing rate: ${alert.itemCategory}") },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }

            if (isUnderwater || isOvershoot) {
                Text(
                    text = "Contact customer now.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RiskMetricRow(label: String, value: String, target: String, isCritical: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Text(target, style = MaterialTheme.typography.labelSmall, color = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SafeRecordsSection(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    records: List<LedgerRecord>,
    onRecordClick: (String) -> Unit
) {
    if (count == 0) return

    Column {
        Surface(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${count} Safe Records", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            records.forEach { record ->
                RecordRow(record = record, onClick = { onRecordClick(record.id) })
            }
        }
    }
}

@Composable
fun DashboardTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        divider = {}
    ) {
        Tab(selected = selectedTab == 0, onClick = { onTabSelected(0) }, text = { Text("Money GIVEN") })
        Tab(selected = selectedTab == 1, onClick = { onTabSelected(1) }, text = { Text("Money TAKEN") })
    }
}

@Composable
fun RecordRow(record: LedgerRecord, onClick: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm") }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(record.transactionId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(record.startDate.format(formatter), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                formatCurrency(record.principalAmount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
