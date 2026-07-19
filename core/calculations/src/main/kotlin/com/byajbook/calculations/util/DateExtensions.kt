package com.byajbook.calculations.util

import java.time.LocalDate

/**
 * Parses a nullable/blank ISO date string, returning null if:
 * - the string is null or blank (handles legacy endDate="" records)
 * - the string fails to parse as LocalDate
 * - the parsed date is in the future (not a valid past targetDate)
 *
 * Use for legacy String? fields (e.g. backup import): dateString.toSafePastDate() ?: LocalDate.now()
 * For LocalDate? domain fields use: endDate?.takeIf { !it.isAfter(LocalDate.now()) } ?: LocalDate.now()
 *
 * Visibility: internal (not private). This function is used by both
 * getDashboard() and calculateRecordFinancials(). A private function would
 * force duplication across files, which is the exact bug this fixes.
 */
internal fun String?.toSafePastDate(): LocalDate? =
    this?.takeIf { it.isNotBlank() }
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.takeIf { d -> !d.isAfter(LocalDate.now()) }
