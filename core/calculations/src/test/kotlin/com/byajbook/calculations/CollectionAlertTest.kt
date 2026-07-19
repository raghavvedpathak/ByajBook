package com.byajbook.calculations

import com.byajbook.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CollectionAlertTest {

    private val baseRecord = LedgerRecord(
        id = "r1",
        transactionId = "TXN-000001",
        customerId = "C1",
        type = RecordType.GIVEN,
        status = RecordStatus.ACTIVE,
        startDate = LocalDateTime.now().minusMonths(6),
        endDate = null,
        principalAmount = 10000.0,
        interestRate = 2.0,
        settledDate = null,
        calculatedInterest = null,
        linkedRecordId = null,
        items = listOf(
            LedgerItem("i1", "r1", "Gold", "Gold 22K", null, 10.0, 91.6, 5000.0, 45800.0, 80.0, 36640.0)
        ),
        payments = emptyList()
    )

    @Test
    fun testComputeCollectionAlerts_CollateralDrop() {
        // Principal 10000, Interest 2% for 6 months = 1200. Total Due = 11200.
        // Current collateral value: Rate drops to 1000/unit -> Value = 10 * 0.916 * 1000 = 916.
        // 916 <= 11200 -> Trigger drop.
        val rates = listOf(ItemRate("rate1", "Gold 22K", 1000.0, LocalDate.now(), LocalDateTime.now()))
        
        val alerts = computeCollectionAlerts(listOf(baseRecord), rates)
        
        assertTrue(alerts.any { it is CollectionAlert.CollateralDrop })
    }

    @Test
    fun testComputeCollectionAlerts_Overshoot() {
        // Principal 10000. Item Value at lending = 45800.
        // In 2 months interest (if 6 months old) -> 8 months = 1600. Total due in 2 mo = 11600.
        // Item value 45800 > 11600 -> No overshoot.
        
        // Let's force overshoot by setting interest rate to 500% (hypothetically)
        val riskyRecord = baseRecord.copy(interestRate = 500.0) 
        // 8 months * 500% = 4000% of 10000 = 400000 interest.
        // 400000 + 10000 = 410000 due. 410000 > 45800 -> Trigger overshoot.
        
        val rates = listOf(ItemRate("rate1", "Gold 22K", 5000.0, LocalDate.now(), LocalDateTime.now()))
        val alerts = computeCollectionAlerts(listOf(riskyRecord), rates)
        
        assertTrue(alerts.any { it is CollectionAlert.OvershootWarning })
    }

    @Test
    fun testComputeCollectionAlerts_RateMissing() {
        val rates = emptyList<ItemRate>()
        val alerts = computeCollectionAlerts(listOf(baseRecord), rates)
        
        assertTrue(alerts.any { it is CollectionAlert.RateMissing })
    }
}
