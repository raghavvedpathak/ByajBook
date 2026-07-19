package com.byajbook.data.debug

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpDatabaseSeeder @Inject constructor() : DatabaseSeeder {
    override suspend fun seed() {
        // No-op in release
    }
}
