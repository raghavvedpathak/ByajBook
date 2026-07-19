package com.byajbook.feature.reports

import androidx.compose.runtime.Composable
import com.byajbook.feature.customers.CustomerDetailScreen

/**
 * Reuses CustomerDetailScreen for the drill-down in Reports flow.
 * Per M-3 FIX: Drill-down on phone screens instead of inline.
 */
@Composable
fun CustomerReportDetailScreen(
    customerId: String,
    onBack: () -> Unit,
    onRecordClick: (String) -> Unit
) {
    CustomerDetailScreen(
        customerId = customerId,
        onBack = onBack,
        onRecordClick = onRecordClick
    )
}
