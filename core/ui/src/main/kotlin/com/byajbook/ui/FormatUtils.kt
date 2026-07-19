package com.byajbook.ui

import java.text.NumberFormat
import java.util.Locale

// Spec Requirement: Centralized Currency Formatting
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    return format.format(amount)
}