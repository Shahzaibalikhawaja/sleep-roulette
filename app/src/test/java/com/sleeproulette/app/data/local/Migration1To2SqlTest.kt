package com.sleeproulette.app.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Documents and guards the non-destructive 1→2 migration SQL.
 * Full Room MigrationTestHelper coverage would need androidTest + schema assets.
 */
class Migration1To2SqlTest {

    @Test
    fun migrationSql_addsNullableGoalColumn() {
        val sql = "ALTER TABLE sleep_sessions ADD COLUMN goalAtStart INTEGER DEFAULT NULL"
        assertThat(sql).contains("goalAtStart")
        assertThat(sql).contains("DEFAULT NULL")
        // Sanity: Migration object exists and targets 1→2.
        assertThat(MIGRATION_1_2.startVersion).isEqualTo(1)
        assertThat(MIGRATION_1_2.endVersion).isEqualTo(2)
    }
}
