package com.byajbook.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Single source of truth for Room migrations.
 * Never call .fallbackToDestructiveMigration().
 */
object Migrations {
    // No migrations yet at v1. 
    // Example for future:
    // val MIGRATION_1_2 = object : Migration(1, 2) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         // Schema changes here
    //     }
    // }
    
    val ALL = arrayOf<Migration>()
}
