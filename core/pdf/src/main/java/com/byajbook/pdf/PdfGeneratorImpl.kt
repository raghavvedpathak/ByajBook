package com.byajbook.pdf

import android.content.Context
import androidx.core.content.FileProvider
import com.byajbook.calculations.calculateRecordFinancials
import com.byajbook.calculations.getCustomerReport
import com.byajbook.domain.model.*
import com.byajbook.domain.service.PdfService
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGeneratorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PdfService {

    private val dateDisplayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimeDisplayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    override suspend fun generateCustomerStatement(
        customer: Customer,
        records: List<LedgerRecord>,
        businessInfo: Settings
    ): String? {
        val file = File(context.cacheDir, "pdfs/statement_${customer.displayId}.pdf")
        file.parentFile?.mkdirs()
        
        val writer = PdfWriter(file)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        // 1. Business Header
        document.add(Paragraph(businessInfo.name).setBold().setFontSize(18f))
        document.add(Paragraph("Customer Statement: ${customer.name} (${customer.displayId})"))
        document.add(Paragraph("Generated on: ${LocalDateTime.now().format(dateTimeDisplayFormat)}"))

        val today = LocalDate.now()
        var grandTotalPrincipal = 0.0
        var grandTotalInterest = 0.0

        records.forEach { record ->
            val financials = calculateRecordFinancials(record, today)
            grandTotalPrincipal += financials.outstandingPrincipal
            grandTotalInterest += financials.outstandingInterest

            document.add(Paragraph("\nRecord: ${record.transactionId} (${record.type.name})").setBold())
            document.add(Paragraph("Start Date: ${record.startDate.format(dateTimeDisplayFormat)}"))
            
            // Record Items Table
            if (record.items.isNotEmpty()) {
                val itemTable = Table(UnitValue.createPointArray(floatArrayOf(2f, 1f, 1f, 2f)))
                itemTable.width = UnitValue.createPercentValue(100f)
                itemTable.addHeaderCell("Item")
                itemTable.addHeaderCell("Weight")
                itemTable.addHeaderCell("Purity")
                itemTable.addHeaderCell("Lending Value")
                
                record.items.forEach { item ->
                    itemTable.addCell(item.name)
                    itemTable.addCell("${item.weight}g")
                    itemTable.addCell("${item.purity}%")
                    itemTable.addCell(item.lendableAmount.toString())
                }
                document.add(itemTable)
            }

            // Payment History Table
            if (record.payments.isNotEmpty()) {
                document.add(Paragraph("Payment History:"))
                val payTable = Table(UnitValue.createPointArray(floatArrayOf(2f, 2f, 2f, 2f)))
                payTable.width = UnitValue.createPercentValue(100f)
                payTable.addHeaderCell("Date")
                payTable.addHeaderCell("Amount")
                payTable.addHeaderCell("Interest Paid")
                payTable.addHeaderCell("Principal Paid")
                
                record.payments.forEach { payment ->
                    payTable.addCell(payment.date.format(dateTimeDisplayFormat))
                    payTable.addCell(payment.amount.toString())
                    payTable.addCell(payment.interestPaid.toString())
                    payTable.addCell(payment.principalPaid.toString())
                }
                document.add(payTable)
            }

            document.add(Paragraph("Record Totals -> Principal: ${financials.outstandingPrincipal} | Interest: ${financials.outstandingInterest} | Total Due: ${financials.totalDue}"))
        }

        // 4. Grand Totals
        document.add(Paragraph("\nOVERALL CUSTOMER BALANCE").setBold().setFontSize(14f))
        document.add(Paragraph("Total Outstanding Principal: $grandTotalPrincipal"))
        document.add(Paragraph("Total Outstanding Interest: $grandTotalInterest"))
        document.add(Paragraph("GRAND TOTAL DUE: ${grandTotalPrincipal + grandTotalInterest}").setBold())

        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        return uri.toString()
    }

    override suspend fun generateAllCustomersReport(
        customers: List<Customer>,
        records: List<LedgerRecord>,
        businessInfo: Settings
    ): String? {
        val file = File(context.cacheDir, "pdfs/all_customers_report.pdf")
        file.parentFile?.mkdirs()

        val writer = PdfWriter(file)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        // 1. Business header
        document.add(Paragraph(businessInfo.name).setBold().setFontSize(18f))
        document.add(Paragraph("All Customers Summary Report"))
        document.add(Paragraph("Generated on: ${LocalDateTime.now().format(dateTimeDisplayFormat)}"))

        val reportData = getCustomerReport(records, customers)

        // 2. Summary table
        val table = Table(UnitValue.createPointArray(floatArrayOf(2f, 1f, 1f, 2f, 2f, 2f)))
        table.width = UnitValue.createPercentValue(100f)
        table.addHeaderCell("Name")
        table.addHeaderCell("ID")
        table.addHeaderCell("Active")
        table.addHeaderCell("Principal")
        table.addHeaderCell("Interest")
        table.addHeaderCell("Total Due")

        var totalP = 0.0
        var totalI = 0.0
        var totalD = 0.0

        reportData.forEach { row ->
            table.addCell(row.customer.name)
            table.addCell(row.customer.displayId)
            table.addCell(row.activeRecordCount.toString())
            table.addCell(row.totalPrincipal.toString())
            table.addCell(row.totalInterestAccrued.toString())
            table.addCell(row.totalDue.toString())
            
            totalP += row.totalPrincipal
            totalI += row.totalInterestAccrued
            totalD += row.totalDue
        }

        // 3. Grand total row
        table.addCell(Paragraph("GRAND TOTALS").setBold())
        table.addCell("")
        table.addCell(reportData.sumOf { it.activeRecordCount }.toString())
        table.addCell(Paragraph(totalP.toString()).setBold())
        table.addCell(Paragraph(totalI.toString()).setBold())
        table.addCell(Paragraph(totalD.toString()).setBold())

        document.add(table)

        // 4. Report footer
        document.add(Paragraph("\nReport Summary:"))
        document.add(Paragraph("Total Customers: ${customers.size}"))
        document.add(Paragraph("Total Active Records: ${reportData.sumOf { it.activeRecordCount }}"))
        document.add(Paragraph("End of Report."))

        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        return uri.toString()
    }
}
