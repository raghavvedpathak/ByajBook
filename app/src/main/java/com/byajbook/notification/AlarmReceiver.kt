package com.byajbook.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Reschedule for the next day
        AlarmScheduler.scheduleDailyAlarm(context)

        // Enqueue the worker to check for overdue records
        val workRequest = OneTimeWorkRequestBuilder<OverdueWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
