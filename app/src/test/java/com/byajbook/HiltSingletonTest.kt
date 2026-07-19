package com.byajbook

import com.byajbook.domain.repository.RecordRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
class HiltSingletonTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repo1: RecordRepository

    @Inject
    lateinit var repo2: RecordRepository

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testRecordRepositoryIsSingleton() {
        // [FIX-SINGLETON-SCOPE-1] CRITICAL: Must be the same instance to preserve Mutex integrity
        assertSame("RecordRepository must be a singleton instance", repo1, repo2)
    }
}
