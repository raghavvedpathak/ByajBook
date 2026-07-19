package com.byajbook

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.byajbook.data.debug.DatabaseSeeder
import com.byajbook.notification.AlarmScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import android.os.StrictMode
import javax.inject.Inject

@HiltAndroidApp
class ByajBookApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskWrites()
                    .penaltyDeath()
                    .build()
            )
        }
        // [PRE-BUILD-ACTION-4] Spec Requirement: Create channels on launch
        createNotificationChannels()
        
        // Run disk operations (preferences and seeding) off the main thread to satisfy StrictMode
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            // Initial Alarm Setup
            setupInitialAlarm()

            // [W2-DELIVERABLE] Debug Seeding - Disabled to ensure 100% clean database
            // runCatching {
            //     databaseSeeder.seed()
            // }.onFailure { e ->
            //     android.util.Log.e("ByajBookApp", "Seeding failed", e)
            // }
        }
    }

    private fun setupInitialAlarm() {
        val prefs = getSharedPreferences("byajbook_prefs", Context.MODE_PRIVATE)
        val alarmScheduled = prefs.getBoolean("alarm_scheduled_v1", false)
        if (!alarmScheduled) {
            AlarmScheduler.scheduleDailyAlarm(this)
            prefs.edit().putBoolean("alarm_scheduled_v1", true).apply()
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Channel 1: 30-day Overdue Alerts (Importance: DEFAULT)
        val alertsChannel = NotificationChannel(
            "byajbook_alerts",
            "Overdue Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(alertsChannel)

        // Channel 2: 2-month Overshoot Warnings (Importance: HIGH)
        val overshootChannel = NotificationChannel(
            "byajbook_overshoot",
            "Collection Warnings",
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(overshootChannel)
    }
}