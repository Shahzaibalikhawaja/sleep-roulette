package com.sleeproulette.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Non-destructive: add nullable goal snapshot; existing sleep rows keep data.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE sleep_sessions ADD COLUMN goalAtStart INTEGER DEFAULT NULL",
        )
    }
}
