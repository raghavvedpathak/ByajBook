package com.byajbook.ui.model

import com.byajbook.domain.model.CollectionAlert
import com.byajbook.domain.model.LedgerRecord

/**
 * PRESENTATION TYPE — defined in :core:ui by architectural decision (v45).
 * Not a domain concept.
 * See spec §13 Step 3 [FIX-ARCH-DEBT-2] for rationale. 
 * Do not move to :core:domain.
 *
 * [FIX-ARCH-DEBT-2] RecordAlertGroup lives in :core:ui.
 * Used by DashboardViewModel to group alerts per record for UI rendering.
 */
data class RecordAlertGroup(
    val record: LedgerRecord,
    val alerts: List<CollectionAlert>
)
