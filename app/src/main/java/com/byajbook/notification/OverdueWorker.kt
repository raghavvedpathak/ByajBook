package com.byajbook.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.byajbook.MainActivity
import com.byajbook.R
import com.byajbook.calculations.computeCollectionAlerts
import com.byajbook.calculations.getOverdue
import com.byajbook.domain.model.CollectionAlert
import com.byajbook.domain.repository.ItemRateRepository
import com.byajbook.domain.repository.RecordRepository
import com.byajbook.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class OverdueWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recordRepository: RecordRepository,
    private val itemRateRepository: ItemRateRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        
        // 1. Overdue Check (30 days)
        val activeRecords = recordRepository.getAllActiveRecordsOnce()
        val latestActivityMap = recordRepository.getActiveRecordLastActivityMap()
        
        val overdueRecords = getOverdue(
            records = activeRecords,
            latestPaymentDates = latestActivityMap,
            today = today,
            thresholdDays = 30
        )

        if (overdueRecords.isNotEmpty()) {
            postOverdueNotification(overdueRecords.size)
        }

        // 2. Overshoot Check
        val activeGivenRecords = activeRecords.filter { it.type == com.byajbook.domain.model.RecordType.GIVEN }
        val currentRates = itemRateRepository.getCurrentRates().first()
        
        val activeGivenIds = activeGivenRecords.map { it.id }
        val totalPaidList = activeGivenIds.chunked(500).flatMap { batch ->
            recordRepository.getTotalPaidByRecordIds(batch)
        }
        val totalPaidMap = totalPaidList.associate { it.recordId to it.totalPaid }

        val alerts = computeCollectionAlerts(
            records = activeGivenRecords,
            rates = currentRates,
            totalPaidMap = totalPaidMap
        )

        val overshootAlerts = alerts.filterIsInstance<CollectionAlert.OvershootWarning>()
        if (overshootAlerts.isNotEmpty()) {
            overshootAlerts.forEach { alert ->
                postOvershootNotification(alert)
            }
        }

        return Result.success()
    }

    private fun postOverdueNotification(count: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // In a real app, you'd add extras to navigate to the specific report
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (count == 1) "1 record is overdue." else "$count records are overdue."
        
        val notification = NotificationCompat.Builder(context, "byajbook_alerts")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Placeholder
            .setContentTitle("Overdue Records")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }

    private fun postOvershootNotification(alert: CollectionAlert.OvershootWarning) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "byajbook_overshoot")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Placeholder
            .setContentTitle("Collection Warning")
            .setContentText("Record ${alert.record.transactionId} is projected to overshoot collateral.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alert.record.id.hashCode(), notification)
    }
}
