package com.byajbook.feature.reports

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byajbook.domain.model.*
import com.byajbook.ui.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    onCustomerDetail: (String) -> Unit
) {
    val activeTabIndex by viewModel.activeTabIndex.collectAsStateWithLifecycle()
    val isFabVisible by viewModel.isFabVisible.collectAsStateWithLifecycle()
    val pdfUri by viewModel.pdfUri.collectAsStateWithLifecycle()
    
    val customerReports by viewModel.customerReports.collectAsStateWithLifecycle()
    val overviewStats by viewModel.overviewStats.collectAsStateWithLifecycle()
    val monthlyEarnings by viewModel.monthlyEarnings.collectAsStateWithLifecycle()
    val overdueRecords by viewModel.overdueRecords.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(pdfUri) {
        pdfUri?.let { uriString ->
            ShareCompat.IntentBuilder(context)
                .setType("application/pdf")
                .setStream(Uri.parse(uriString))
                .setChooserTitle("Share Report")
                .startChooser()
            viewModel.onPdfConsumed()
        }
    }

    Scaffold(
        topBar = {
            Column {
                PrimaryTabRow(selectedTabIndex = activeTabIndex) {
                    Tab(
                        selected = activeTabIndex == 0,
                        onClick = { viewModel.onTabSelected(0) },
                        text = { Text("Overview") }
                    )
                    Tab(
                        selected = activeTabIndex == 1,
                        onClick = { viewModel.onTabSelected(1) },
                        text = { Text("Customer") }
                    )
                    Tab(
                        selected = activeTabIndex == 2,
                        onClick = { viewModel.onTabSelected(2) },
                        text = { Text("Monthly") }
                    )
                    Tab(
                        selected = activeTabIndex == 3,
                        onClick = { viewModel.onTabSelected(3) },
                        text = { Text("Overdue") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (isFabVisible) {
                FloatingActionButton(onClick = { viewModel.onFabClicked() }) {
                    Icon(Icons.Default.Share, contentDescription = "Share PDF")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (activeTabIndex) {
                0 -> OverviewTabContent(overviewStats)
                1 -> CustomerReportTabContent(customerReports, onCustomerDetail)
                2 -> MonthlyEarningsContent(monthlyEarnings)
                3 -> OverdueListContent(overdueRecords)
            }
        }
    }
}

@Composable
fun OverviewTabContent(stats: DashboardStats?) {
    if (stats == null) return
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Business Totals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            GrandTotalCard(
                label = "Total Principal Out",
                amount = stats.totalPrincipalGiven,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            GrandTotalCard(
                label = "Total Interest Accrued",
                amount = stats.totalInterestAccruedGiven,
                color = Color(0xFF2E7D32)
            )
        }
        item {
            GrandTotalCard(
                label = "Grand Total Due",
                amount = stats.totalDueGiven,
                color = Color(0xFFC62828)
            )
        }
    }
}

@Composable
fun GrandTotalCard(label: String, amount: Double, color: Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(formatCurrency(amount), style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CustomerReportTabContent(
    reports: List<CustomerReport>,
    onDetail: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(reports) { report ->
            ListItem(
                headlineContent = { Text(report.customer.name, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Active: ${report.activeRecordCount} · Due: ${formatCurrency(report.totalDue)}") },
                trailingContent = { Text(formatCurrency(report.totalDue), fontWeight = FontWeight.Bold) },
                modifier = Modifier.clickable { onDetail(report.customer.id) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun MonthlyEarningsContent(earnings: List<MonthlyEarning>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Interest Received", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        items(earnings.reversed()) { earning ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${earning.month.month.name} ${earning.month.year}")
                Text(formatCurrency(earning.interestReceived), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun OverdueListContent(records: List<OverdueRecord>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (records.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records overdue beyond threshold.", color = Color.Gray)
                }
            }
        }
        items(records) { overdue ->
            ListItem(
                headlineContent = { Text("Txn: ${overdue.record.transactionId}", fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Last activity: ${overdue.lastActivityDate}") },
                trailingContent = { 
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${overdue.daysSinceActivity} days", color = Color.Red, fontWeight = FontWeight.Bold)
                        Text("Overdue", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                    }
                }
            )
            HorizontalDivider()
        }
    }
}
