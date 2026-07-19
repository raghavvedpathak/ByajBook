package com.byajbook.domain.service

import com.byajbook.domain.model.Customer
import com.byajbook.domain.model.LedgerRecord
import com.byajbook.domain.model.Settings

interface PdfService {
    suspend fun generateCustomerStatement(
        customer: Customer,
        records: List<LedgerRecord>,
        businessInfo: Settings
    ): String?

    suspend fun generateAllCustomersReport(
        customers: List<Customer>,
        records: List<LedgerRecord>,
        businessInfo: Settings
    ): String?
}
