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
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGeneratorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PdfService {

    private val dateDisplayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimeDisplayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")

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

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        // 1. Business Header
        document.add(Paragraph(businessInfo.name).setBold().setFontSize(20f))
        document.add(Paragraph("Address: ${businessInfo.address} | Phone: ${businessInfo.phone}").setFontSize(10f))
        document.add(Paragraph("Generated on: ${LocalDateTime.now().format(dateTimeDisplayFormat)}").setFontSize(10f))
        
        // 2. Customer Info Header
        document.add(Paragraph("\nCUSTOMER LEDGER STATEMENT").setBold().setFontSize(14f))
        document.add(Paragraph("Name: ${customer.name} (ID: ${customer.displayId})").setFontSize(11f))
        document.add(Paragraph("Phone: ${customer.phone} | Address: ${customer.address}").setFontSize(11f))
        document.add(Paragraph("----------------------------------------------------------------------------------------------------"))

        val today = LocalDate.now()
        var grandTotalPrincipal = 0.0
        var grandTotalInterest = 0.0

        records.forEach { record ->
            val financials = calculateRecordFinancials(record, today)
            grandTotalPrincipal += financials.outstandingPrincipal
            grandTotalInterest += financials.outstandingInterest

            // Record Details Header
            document.add(Paragraph("\nRecord: ${record.transactionId} [${record.type.name}]").setBold().setFontSize(12f))
            document.add(Paragraph("Start Date: ${record.startDate.format(dateTimeDisplayFormat)} | Interest Rate: ${record.interestRate}% per month").setFontSize(10f))
            
            // Record Items Table
            if (record.items.isNotEmpty()) {
                document.add(Paragraph("Collateral Items:").setFontSize(10f).setBold())
                val itemTable = Table(UnitValue.createPointArray(floatArrayOf(3f, 1.5f, 1.5f, 2f)))
                itemTable.width = UnitValue.createPercentValue(100f)
                itemTable.addHeaderCell("Item Name")
                itemTable.addHeaderCell("Weight")
                itemTable.addHeaderCell("Purity")
                itemTable.addHeaderCell("Lending Value")
                
                record.items.forEach { item ->
                    itemTable.addCell(item.name)
                    itemTable.addCell("${item.weight}g")
                    itemTable.addCell("${item.purity}%")
                    itemTable.addCell(currencyFormat.format(item.lendableAmount))
                }
                document.add(itemTable)
            }

            // Payment History Table
            if (record.payments.isNotEmpty()) {
                document.add(Paragraph("Payment History:").setFontSize(10f).setBold())
                val payTable = Table(UnitValue.createPointArray(floatArrayOf(2.5f, 2f, 2.5f, 2.5f)))
                payTable.width = UnitValue.createPercentValue(100f)
                payTable.addHeaderCell("Date & Time")
                payTable.addHeaderCell("Amount Paid")
                payTable.addHeaderCell("Interest Paid")
                payTable.addHeaderCell("Principal Paid")
                
                record.payments.forEach { payment ->
                    payTable.addCell(payment.date.format(dateTimeDisplayFormat))
                    payTable.addCell(currencyFormat.format(payment.amount))
                    payTable.addCell(currencyFormat.format(payment.interestPaid))
                    payTable.addCell(currencyFormat.format(payment.principalPaid))
                }
                document.add(payTable)
            }

            // Record Totals Section (Structured table)
            val recordTotalTable = Table(UnitValue.createPointArray(floatArrayOf(2.5f, 2.5f, 2.5f, 2.5f)))
            recordTotalTable.width = UnitValue.createPercentValue(100f)
            recordTotalTable.addCell("Principal: ${currencyFormat.format(record.principalAmount)}")
            recordTotalTable.addCell("Paid Principal: ${currencyFormat.format(financials.principalPaid)}")
            recordTotalTable.addCell("Paid Interest: ${currencyFormat.format(financials.interestPaid)}")
            recordTotalTable.addCell("Total Due: ${currencyFormat.format(financials.totalDue)}")
            document.add(recordTotalTable)
        }

        // 4. Grand Totals Summary Footer
        document.add(Paragraph("\nOVERALL STATEMENT SUMMARY").setBold().setFontSize(14f))
        document.add(Paragraph("Total Outstanding Principal: ${currencyFormat.format(grandTotalPrincipal)}"))
        document.add(Paragraph("Total Outstanding Interest: ${currencyFormat.format(grandTotalInterest)}"))
        document.add(Paragraph("GRAND TOTAL DUE: ${currencyFormat.format(grandTotalPrincipal + grandTotalInterest)}").setBold().setFontSize(12f))

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

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        // 1. Business header
        document.add(Paragraph(businessInfo.name).setBold().setFontSize(20f))
        document.add(Paragraph("Address: ${businessInfo.address} | Phone: ${businessInfo.phone}").setFontSize(10f))
        document.add(Paragraph("All Customers Summary Report"))
        document.add(Paragraph("Generated on: ${LocalDateTime.now().format(dateTimeDisplayFormat)}").setFontSize(10f))
        document.add(Paragraph("----------------------------------------------------------------------------------------------------"))

        val reportData = getCustomerReport(records, customers)

        // 2. Summary table
        val table = Table(UnitValue.createPointArray(floatArrayOf(2f, 1.5f, 1f, 2f, 2f, 2f)))
        table.width = UnitValue.createPercentValue(100f)
        table.addHeaderCell("Customer Name")
        table.addHeaderCell("Customer ID")
        table.addHeaderCell("Active")
        table.addHeaderCell("Total Principal")
        table.addHeaderCell("Total Interest")
        table.addHeaderCell("Total Due")

        var totalP = 0.0
        var totalI = 0.0
        var totalD = 0.0
        var totalActiveRecords = 0

        reportData.forEach { row ->
            table.addCell(row.customer.name)
            table.addCell(row.customer.displayId)
            table.addCell(row.activeRecordCount.toString())
            table.addCell(currencyFormat.format(row.totalPrincipal))
            table.addCell(currencyFormat.format(row.totalInterestAccrued))
            table.addCell(currencyFormat.format(row.totalDue))
            
            totalP += row.totalPrincipal
            totalI += row.totalInterestAccrued
            totalD += row.totalDue
            totalActiveRecords += row.activeRecordCount
        }

        // 3. Grand total row
        table.addCell(Paragraph("GRAND TOTALS").setBold())
        table.addCell("")
        table.addCell(totalActiveRecords.toString())
        table.addCell(Paragraph(currencyFormat.format(totalP)).setBold())
        table.addCell(Paragraph(currencyFormat.format(totalI)).setBold())
        table.addCell(Paragraph(currencyFormat.format(totalD)).setBold())
        document.add(table)

        // 4. Report footer
        document.add(Paragraph("\nReport Summary Footer").setBold().setFontSize(11f))
        document.add(Paragraph("Total Customers: ${customers.size}"))
        document.add(Paragraph("Total Active Records: $totalActiveRecords"))
        document.add(Paragraph("Generated on: ${LocalDateTime.now().format(dateTimeDisplayFormat)}").setFontSize(10f))

        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        return uri.toString()
    }
}
