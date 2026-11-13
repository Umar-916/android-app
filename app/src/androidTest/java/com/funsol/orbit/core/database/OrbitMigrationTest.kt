package com.funsol.orbit.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrbitMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OrbitDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun createVersion1_hasExpectedTablesAndSurvivesReopen() {
        helper.createDatabase(OrbitDatabase.NAME, 1).apply {
            execSQL("INSERT INTO projects (id,name,description,color,status,createdAt,updatedAt) VALUES ('p','Project','',0,'ACTIVE',1,1)")
            execSQL("INSERT INTO tasks (id,title,description,priority,status,createdAt,updatedAt,recurrenceType,recurrenceInterval,recurrenceWeekdays,recurrenceAnchorDay,projectId) VALUES ('t','Task','','MEDIUM','TODO',1,1,'NONE',1,0,1,'p')")
            close()
        }
        val db = helper.runMigrationsAndValidate(OrbitDatabase.NAME, 1, true, *OrbitDatabase.MIGRATIONS)
        db.query("SELECT COUNT(*) FROM tasks WHERE projectId = 'p'").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        db.close()
    }

    @Test
    fun everyDeclaredMigrationHasAContiguousVersionRange() {
        var expected = 1
        OrbitDatabase.MIGRATIONS.sortedBy { it.startVersion }.forEach {
            assertEquals(expected, it.startVersion)
            expected = it.endVersion
        }
        assertEquals(OrbitDatabase.VERSION, expected)
    }
}
