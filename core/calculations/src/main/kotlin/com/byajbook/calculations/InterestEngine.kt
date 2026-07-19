package com.byajbook.calculations

import com.byajbook.domain.model.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * [FIX-DATEINPUT-1] Core calculation logic for interest.
 */

/**
 * Core ledger engine. Full algorithm defined in §5.2.2.
 * This function handles the "Settled-Record Fast-Path" defined in §5.2.1.
 */
fun calculateRecordFinancials(record: LedgerRecord, targetDate: LocalDate): Financials {
    // 5.2.1 Settled-Record Fast-Path
    if (record.status == RecordStatus.SETTLED) {
        val calcInterest = record.calculatedInterest
        if (calcInterest != null) {
            val totalPaid = record.payments.sumOf { it.amount }
            val interestPaid = record.payments.sumOf { it.interestPaid }
            val principalPaid = record.payments.sumOf { it.principalPaid }
            
            return Financials(
                totalInterest = calcInterest,
                totalPaid = totalPaid,
                interestPaid = interestPaid,
                principalPaid = principalPaid,
                outstandingInterest = maxOf(0.0, calcInterest - interestPaid),
                outstandingPrincipal = maxOf(0.0, record.principalAmount - principalPaid),
                totalDue = 0.0 // Settled records have 0 due by definition
            )
        } else {
            // Edge case — settled with null calculatedInterest (legacy backup)
            val settledTarget: LocalDate = record.settledDate ?: return Financials(
                totalInterest = 0.0,
                totalPaid = record.payments.sumOf { it.amount },
                interestPaid = record.payments.sumOf { it.interestPaid },
                principalPaid = record.payments.sumOf { it.principalPaid },
                outstandingInterest = 0.0,
                outstandingPrincipal = maxOf(0.0, record.principalAmount - record.payments.sumOf { it.principalPaid }),
                totalDue = maxOf(0.0, record.principalAmount - record.payments.sumOf { it.principalPaid })
            )
            
            // If settledDate is available, run the calculation with settledDate as targetDate.
            return calculateRecordFinancialsInternal(record, settledTarget)
        }
    }

    // Active-record path
    return calculateRecordFinancialsInternal(record, targetDate)
}

/**
 * Splits a payment amount into interest and principal portions using interest-first rule.
 * @param paymentAmount The amount the customer is paying now (must be > 0).
 * @param outstandingInterest Current accrued interest minus interest already paid.
 * Compute via calculateRecordFinancials(record, today).outstandingInterest.
 * @return Pair(interestPaid, principalPaid)
 */
fun allocatePayment(
    paymentAmount: Double,
    outstandingInterest: Double
): Pair<Double, Double> {
    val interestPaid = minOf(paymentAmount, outstandingInterest)
    val principalPaid = paymentAmount - interestPaid
    return Pair(interestPaid, principalPaid)
}

private fun calculateRecordFinancialsInternal(record: LedgerRecord, targetDate: LocalDate): Financials {
    // Step 1: Determine effective target date.
    // Use endDate?.takeIf { !it.isAfter(LocalDate.now()) } since endDate is LocalDate? in domain model.
    val effectiveTarget = record.endDate?.takeIf { !it.isAfter(LocalDate.now()) } ?: targetDate

    // Step 2: Calculate total accrued interest.
    val totalInterest = calculateInterestForPeriod(
        principal = record.principalAmount,
        rate = record.interestRate,
        start = record.startDate.toLocalDate(), // [FIX-TIMESTAMP-CALC-1]
        end = effectiveTarget
    )

    // Step 3: Sum payments (already split by interest-first allocation at recording time).
    val interestPaid = record.payments.sumOf { it.interestPaid }
    val principalPaid = record.payments.sumOf { it.principalPaid }
    val totalPaid = record.payments.sumOf { it.amount }

    // Step 4: Derive outstanding amounts (floored at 0 — no negative balances).
    val outstandingInterest = maxOf(0.0, totalInterest - interestPaid)
    val outstandingPrincipal = maxOf(0.0, record.principalAmount - principalPaid)

    return Financials(
        totalInterest = totalInterest,
        totalPaid = totalPaid,
        interestPaid = interestPaid,
        principalPaid = principalPaid,
        outstandingInterest = outstandingInterest,
        outstandingPrincipal = outstandingPrincipal,
        totalDue = outstandingPrincipal + outstandingInterest
    )
}

/**
 * Standard interest formula: principal * rate * months / 100
 */
fun calculateInterestForPeriod(principal: Double, rate: Double, start: LocalDate, end: LocalDate): Double {
    val months = getMonthsBetween(start, end)
    return (principal * rate * months) / 100.0
}

/**
 * [FIX-LEDGERITEM-NULLABILITY-1]
 * itemValue = weight * (purity/100) * rate;
 * lendableAmount = itemValue * (lendPercentage/100).
 */
fun calculateItemValue(item: LedgerItem): Double {
    return item.weight * (item.purity / 100.0) * item.rate
}

fun calculateTotalItemValue(items: List<LedgerItem>): Double {
    return items.sumOf { calculateItemValue(it) }
}

/**
 * Aggregate totals for summary cards.
 */
fun getDashboard(records: List<LedgerRecord>): DashboardStats {
    var principalGiven = 0.0
    var interestGiven = 0.0
    var principalTaken = 0.0
    var interestTaken = 0.0
    
    val today = LocalDate.now()

    records.forEach { record ->
        val financials = calculateRecordFinancials(record, today)
        if (record.type == RecordType.GIVEN) {
            principalGiven += financials.outstandingPrincipal
            interestGiven += financials.outstandingInterest
        } else {
            principalTaken += financials.outstandingPrincipal
            interestTaken += financials.outstandingInterest
        }
    }

    return DashboardStats(
        totalPrincipalGiven = principalGiven,
        totalInterestAccruedGiven = interestGiven,
        totalDueGiven = principalGiven + interestGiven,
        totalPrincipalTaken = principalTaken,
        totalInterestAccruedTaken = interestTaken,
        totalDueTaken = principalTaken + interestTaken
    )
}

/**
 * Per-customer report rollup.
 */
fun getCustomerReport(records: List<LedgerRecord>, customers: List<Customer>): List<CustomerReport> {
    val today = LocalDate.now()
    val recordMap = records.groupBy { it.customerId }
    
    return customers.map { customer ->
        val customerRecords = recordMap[customer.id] ?: emptyList()
        val activeRecords = customerRecords.filter { it.status == RecordStatus.ACTIVE }
        
        var totalPrincipal = 0.0
        var totalInterest = 0.0
        
        activeRecords.forEach { record ->
            val financials = calculateRecordFinancials(record, today)
            totalPrincipal += record.principalAmount
            totalInterest += financials.totalInterest
        }
        
        CustomerReport(
            customer = customer,
            activeRecordCount = activeRecords.size,
            totalPrincipal = totalPrincipal,
            totalInterestAccrued = totalInterest,
            totalDue = totalPrincipal + totalInterest
        )
    }
}

/**
 * CASH-BASIS: counts only interest actually PAID.
 */
fun getMonthlyInterest(records: List<LedgerRecord>): List<MonthlyEarning> {
    val earningsMap = mutableMapOf<YearMonth, Double>()
    
    records.flatMap { it.payments }
        .filter { it.interestPaid > 0 }
        .forEach { payment ->
            val month = YearMonth.from(payment.date.toLocalDate())
            earningsMap[month] = (earningsMap[month] ?: 0.0) + payment.interestPaid
        }
    
    return earningsMap.map { MonthlyEarning(it.key, it.value) }
        .sortedBy { it.month }
}

/**
 * Activity-based overdue logic.
 */
fun getOverdue(
    records: List<LedgerRecord>,
    latestPaymentDates: Map<String, LocalDate?>,
    today: LocalDate,
    thresholdDays: Int
): List<OverdueRecord> {
    return records.filter { it.status == RecordStatus.ACTIVE }
        .map { record ->
            val lastActivityDate = latestPaymentDates[record.id] ?: record.startDate.toLocalDate()
            val daysSince = ChronoUnit.DAYS.between(lastActivityDate, today)
            OverdueRecord(record, daysSince, lastActivityDate)
        }
        .filter { it.daysSinceActivity > thresholdDays }
        .sortedByDescending { it.daysSinceActivity }
}

/**
 * [FIX-DEV-COMBINESUSPEND-1] computeCollectionAlerts full implementation.
 * 
 * Sort order:
 * (1) BOTH OvershootWarning and CollateralDrop
 * (2) OvershootWarning only
 * (3) CollateralDrop only
 * (4) RateMissing
 */
fun computeCollectionAlerts(
    records: List<LedgerRecord>,
    rates: List<ItemRate>,
    totalPaidMap: Map<String, Double> = emptyMap()
): List<CollectionAlert> {
    // IMPORTANT: totalPaid is always sourced from the totalPaidMap parameter.
    // Do NOT use record.payments.sumOf{} here — it would bypass the pre-computed
    // aggregates and break the Worker path (which passes a pre-fetched map).
    
    val today = LocalDate.now()
    val rateMap = rates.associateBy { it.itemCategory }
    val alerts = mutableListOf<CollectionAlert>()

    records.filter { it.status == RecordStatus.ACTIVE && it.type == RecordType.GIVEN }.forEach { record ->
        var totalCurrentCollateralValue = 0.0
        var itemValueAtLending = 0.0
        val missingCategories = mutableSetOf<String>()

        record.items.forEach { item ->
            val currentRate = rateMap[item.itemCategory]
            if (currentRate != null) {
                totalCurrentCollateralValue += item.weight * (item.purity / 100.0) * currentRate.ratePerUnit
            } else {
                missingCategories.add(item.itemCategory)
            }
            itemValueAtLending += item.itemValue
        }

        // Financials for today
        val financials = calculateRecordFinancials(record, today)
        
        // Alert 1: Collateral Drop
        val hasDrop = totalCurrentCollateralValue <= financials.totalDue
        if (hasDrop) {
            alerts.add(CollectionAlert.CollateralDrop(record, totalCurrentCollateralValue, financials.totalDue))
        }

        // Alert 2: Rate Missing
        missingCategories.forEach { category ->
            alerts.add(CollectionAlert.RateMissing(record, category))
        }

        // Alert 3: Overshoot Warning (Projected 2 months)
        val projectionDate = today.plusMonths(2)
        val projectedInterest = calculateInterestForPeriod(
            principal = record.principalAmount,
            rate = record.interestRate,
            start = record.startDate.toLocalDate(),
            end = projectionDate
        )
        val totalPaid = totalPaidMap.getOrDefault(record.id, 0.0)
        val projectedOutstanding = record.principalAmount + projectedInterest - totalPaid
        
        val hasOvershoot = projectedOutstanding >= itemValueAtLending
        if (hasOvershoot) {
            alerts.add(CollectionAlert.OvershootWarning(record, projectedOutstanding, itemValueAtLending))
        }
    }

    // Auth 4-group sort order
    return alerts.sortedWith { a, b ->
        val scoreA = getAlertSeverityScore(a, alerts)
        val scoreB = getAlertSeverityScore(b, alerts)
        
        if (scoreA != scoreB) {
            scoreA.compareTo(scoreB)
        } else {
            // Sort within group by gap
            getAlertGap(b).compareTo(getAlertGap(a))
        }
    }
}

private fun getAlertSeverityScore(alert: CollectionAlert, allAlerts: List<CollectionAlert>): Int {
    val record = when (alert) {
        is CollectionAlert.CollateralDrop -> alert.record
        is CollectionAlert.OvershootWarning -> alert.record
        is CollectionAlert.RateMissing -> alert.record
    }
    
    val recordAlerts = allAlerts.filter { 
        when(it) {
            is CollectionAlert.CollateralDrop -> it.record.id == record.id
            is CollectionAlert.OvershootWarning -> it.record.id == record.id
            is CollectionAlert.RateMissing -> it.record.id == record.id
        }
    }
    
    val hasDrop = recordAlerts.any { it is CollectionAlert.CollateralDrop }
    val hasOvershoot = recordAlerts.any { it is CollectionAlert.OvershootWarning }
    
    return when {
        hasDrop && hasOvershoot -> 1
        hasOvershoot -> 2
        hasDrop -> 3
        else -> 4
    }
}

private fun getAlertGap(alert: CollectionAlert): Double {
    return when (alert) {
        is CollectionAlert.CollateralDrop -> alert.totalDue - alert.currentCollateralValue
        is CollectionAlert.OvershootWarning -> alert.projectedOutstanding - alert.itemValueAtLending
        is CollectionAlert.RateMissing -> 0.0
    }
}

/**
 * getMonthsBetween has two branches: when endDay >= startDay (positive dayDiff), the threshold is
 * hardcoded 15 — >15 days = +1 month, >0 days = +0.5 month. When endDay < startDay (negative
 * dayDiff, month rollover), totalMonths is decremented by 1 and adjustedDays (daysInPrevMonth -
 * startDay + endDay) is tested against 15.
 * daysInPrevMonth must be computed as start.plusMonths(totalMonths.toLong()).lengthOfMonth()
 * AFTER the totalMonths -= 1 decrement.
 */
fun getMonthsBetween(start: LocalDate, end: LocalDate): Double {
    val years = end.year - start.year
    val months = end.monthValue - start.monthValue
    var totalMonths = (years * 12 + months).toDouble()
    
    val startDay = start.dayOfMonth
    val endDay = end.dayOfMonth
    val dayDiff = endDay - startDay
    
    if (dayDiff < 0) {
        totalMonths -= 1
        // daysInPrevMonth must be computed AFTER the totalMonths -= 1 decrement.
        val daysInPrevMonth = start.plusMonths(totalMonths.toLong()).lengthOfMonth()
        val adjustedDays = daysInPrevMonth - startDay + endDay
        totalMonths += when {
            adjustedDays > 15 -> 1.0
            adjustedDays > 0 -> 0.5
            else -> 0.0
        }
    } else {
        totalMonths += when {
            dayDiff > 15 -> 1.0
            dayDiff > 0 -> 0.5
            else -> 0.0
        }
    }
    return maxOf(0.0, totalMonths)
}
