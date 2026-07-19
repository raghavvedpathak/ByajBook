package com.byajbook

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ByajBookApplicationTest {

    @Test
    fun testNotificationChannelsCreated() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val alertsChannel = nm.getNotificationChannel("byajbook_alerts")
        val overshootChannel = nm.getNotificationChannel("byajbook_overshoot")
        
        assertNotNull("Overdue Alerts channel should exist", alertsChannel)
        assertNotNull("Collection Warnings channel should exist", overshootChannel)
    }
}
