package com.funsol.orbit.feature.settings

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

class BackupIoTest {
    @Test
    fun fileNamesUseIsoDates() {
        val date = LocalDate.of(2026, 3, 9)
        assertEquals("orbit-backup-2026-03-09.json", BackupIo.backupFileName(date))
        assertEquals("orbit-tasks-2026-03-09.csv", BackupIo.tasksCsvFileName(date))
        assertEquals("orbit-habits-2026-03-09.csv", BackupIo.habitsCsvFileName(date))
    }

    @Test
    fun readLimitedReturnsAllBytesWithinTheLimit() {
        val data = ByteArray(50_000) { (it % 251).toByte() }
        assertArrayEquals(data, BackupIo.readLimited(ByteArrayInputStream(data), limit = 50_000))
    }

    @Test
    fun readLimitedRejectsStreamsOverTheLimit() {
        val data = ByteArray(1_001)
        assertNull(BackupIo.readLimited(ByteArrayInputStream(data), limit = 1_000))
    }

    @Test
    fun emptyStreamIsReadAsEmpty() {
        assertEquals(0, BackupIo.readLimited(ByteArrayInputStream(ByteArray(0)))!!.size)
        assertNotNull(BackupIo.readLimited(ByteArrayInputStream(ByteArray(0))))
    }

    @Test
    fun restoreLimitIsTwentyFiveMegabytes() {
        assertEquals(25 * 1024 * 1024, BackupIo.MAX_RESTORE_BYTES)
    }
}
