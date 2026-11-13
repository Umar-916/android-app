package com.funsol.orbit.feature.goals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class GoalNumbersTest {
    private fun ok(text: String): Double = (GoalNumbers.parse(text) as NumberParse.Ok).value

    private fun error(result: NumberParse): NumberError = (result as NumberParse.Error).error

    @Test
    fun acceptsDotAndCommaDecimalSeparators() {
        assertEquals(12.5, ok("12.5"), 0.0)
        assertEquals(12.5, ok("12,5"), 0.0)
        assertEquals(12.5, ok("  12,5 "), 0.0)
        assertEquals(0.5, ok(".5"), 0.0)
        assertEquals(5.0, ok("5."), 0.0)
        assertEquals(0.0, ok("0"), 0.0)
    }

    @Test
    fun handlesGroupingSeparators() {
        assertEquals(1234.5, ok("1,234.5"), 0.0)
        assertEquals(1234.5, ok("1.234,5"), 0.0)
        assertEquals(1234567.0, ok("1,234,567"), 0.0)
        assertEquals(1000.0, ok("1 000"), 0.0)
    }

    @Test
    fun acceptsNonLatinDigits() {
        assertEquals(3.5, ok("٣٫٥"), 0.0) // Arabic-Indic digits with Arabic decimal separator
        assertEquals(12.0, ok("۱۲"), 0.0) // Extended Arabic-Indic (Urdu) digits
    }

    @Test
    fun rejectsEmptyInput() {
        assertEquals(NumberError.EMPTY, error(GoalNumbers.parse("")))
        assertEquals(NumberError.EMPTY, error(GoalNumbers.parse("   ")))
    }

    @Test
    fun rejectsGarbage() {
        listOf("abc", "12abc", "1e5", "NaN", "Infinity", "-Infinity", ".", ",", "-", "1-2", "--3", "0x10", "12 apples", "٪").forEach {
            assertEquals("input: $it", NumberError.INVALID, error(GoalNumbers.parse(it)))
        }
    }

    @Test
    fun rejectsNegativeNumbers() {
        assertEquals(NumberError.NEGATIVE, error(GoalNumbers.parse("-5")))
        assertEquals(NumberError.NEGATIVE, error(GoalNumbers.parse("-0,5")))
        assertEquals(NumberError.NEGATIVE, error(GoalNumbers.parseCurrent("-1")))
    }

    @Test
    fun rejectsHugeNumbers() {
        assertEquals(NumberError.TOO_LARGE, error(GoalNumbers.parse("9999999999999999")))
        assertEquals(NumberError.TOO_LARGE, error(GoalNumbers.parse("1" + "0".repeat(400))))
        assertEquals(1_000_000_000_000.0, ok("1000000000000"), 0.0)
    }

    @Test
    fun targetMustBeGreaterThanZero() {
        assertEquals(NumberError.ZERO, error(GoalNumbers.parseTarget("0")))
        assertEquals(NumberError.ZERO, error(GoalNumbers.parseTarget("0.0")))
        assertEquals(NumberError.ZERO, error(GoalNumbers.parseTarget("0,00")))
        assertEquals(NumberError.NEGATIVE, error(GoalNumbers.parseTarget("-3")))
        assertEquals(NumberError.EMPTY, error(GoalNumbers.parseTarget("")))
        assertEquals(0.001, (GoalNumbers.parseTarget("0.001") as NumberParse.Ok).value, 0.0)
    }

    @Test
    fun currentMayBeZero() {
        assertEquals(0.0, (GoalNumbers.parseCurrent("0") as NumberParse.Ok).value, 0.0)
        assertEquals(NumberError.EMPTY, error(GoalNumbers.parseCurrent("")))
    }

    @Test
    fun progressDialogSetAndAdd() {
        assertEquals(7.0, (GoalNumbers.resolveProgress(ProgressMode.SET, "7", 10.0) as NumberParse.Ok).value, 0.0)
        assertEquals(15.5, (GoalNumbers.resolveProgress(ProgressMode.ADD, "5,5", 10.0) as NumberParse.Ok).value, 0.0)
        assertEquals(NumberError.ZERO, error(GoalNumbers.resolveProgress(ProgressMode.ADD, "0", 10.0)))
        assertEquals(NumberError.NEGATIVE, error(GoalNumbers.resolveProgress(ProgressMode.ADD, "-2", 10.0)))
        assertEquals(NumberError.INVALID, error(GoalNumbers.resolveProgress(ProgressMode.SET, "ten", 10.0)))
        assertEquals(
            NumberError.TOO_LARGE,
            error(GoalNumbers.resolveProgress(ProgressMode.ADD, "1000000000000", GoalNumbers.MAX_VALUE)),
        )
    }

    @Test
    fun formatHasNoTrailingZero() {
        assertEquals("12", GoalNumbers.format(12.0, Locale.US))
        assertEquals("12.5", GoalNumbers.format(12.5, Locale.US))
        assertEquals("1,234.57", GoalNumbers.format(1234.567, Locale.US))
        assertEquals("0", GoalNumbers.format(0.0, Locale.US))
        assertEquals("0", GoalNumbers.format(Double.NaN, Locale.US))
    }

    @Test
    fun formatIsLocaleAware() {
        assertEquals("12,5", GoalNumbers.format(12.5, Locale.GERMANY))
    }

    @Test
    fun editTextIsPlainAndRoundTrips() {
        assertEquals("100", GoalNumbers.toEditText(100.0))
        assertEquals("12.5", GoalNumbers.toEditText(12.5))
        assertEquals("0", GoalNumbers.toEditText(0.0))
        assertEquals("1000000000000", GoalNumbers.toEditText(1e12))
        assertEquals("0", GoalNumbers.toEditText(Double.NaN))
        listOf(0.0, 3.0, 12.5, 0.125, 99999.99).forEach {
            assertTrue(ok(GoalNumbers.toEditText(it)) == it)
        }
    }
}
