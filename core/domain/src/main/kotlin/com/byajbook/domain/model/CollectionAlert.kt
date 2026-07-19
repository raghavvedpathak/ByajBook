package com.byajbook.domain.model

/**
 * [FIX-ARCH-COLLALERT-1] CollectionAlert sealed class.
 * This type is the return value of computeCollectionAlerts() in :core:calculations
 * and is consumed by DashboardViewModel in :feature:dashboard.
 */
sealed class CollectionAlert {
    /** 
     * Collateral’s current live market value has dropped at or below totalDue 
     * (principal + accrued interest - payments). 
     */
    data class CollateralDrop(
        val record: LedgerRecord,
        val currentCollateralValue: Double,
        val totalDue: Double
    ) : CollectionAlert()

    /** 
     * No rate exists in ItemRateRepository for this item’s category — item excluded from collateral sum. 
     */
    data class RateMissing(
        val record: LedgerRecord,
        val itemCategory: String
    ) : CollectionAlert()

    /** 
     * Projected outstanding in 2 months will exceed item value at time of lending. 
     */
    data class OvershootWarning(
        val record: LedgerRecord,
        val projectedOutstanding: Double,
        val itemValueAtLending: Double
    ) : CollectionAlert()
}
