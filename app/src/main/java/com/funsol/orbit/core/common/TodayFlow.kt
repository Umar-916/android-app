package com.funsol.orbit.core.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

fun todayFlow(
    zone: () -> ZoneId = { ZoneId.systemDefault() },
    nowProvider: (ZoneId) -> ZonedDateTime = { ZonedDateTime.now(it) },
    maxWaitMillis: Long = 60_000L,
): Flow<LocalDate> = flow {
    while (true) {
        val z = zone()
        val now = nowProvider(z)
        emit(now.toLocalDate())
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(z)
        val untilMidnight = Duration.between(now, nextMidnight).toMillis()
        delay(untilMidnight.coerceIn(1_000L, maxWaitMillis))
    }
}.distinctUntilChanged()

fun millisUntilMidnight(now: ZonedDateTime): Long =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).toMillis()
