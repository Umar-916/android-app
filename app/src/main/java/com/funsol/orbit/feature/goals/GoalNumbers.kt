package com.funsol.orbit.feature.goals

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

enum class NumberError { EMPTY, INVALID, NEGATIVE, ZERO, TOO_LARGE }

sealed interface NumberParse {
    data class Ok(val value: Double) : NumberParse
    data class Error(val error: NumberError) : NumberParse
}

enum class ProgressMode { SET, ADD }

object GoalNumbers {
    const val MAX_VALUE = 1_000_000_000_000.0

    internal fun normalize(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        val sb = StringBuilder()
        var negative = false
        trimmed.forEachIndexed { index, c ->
            when {
                c == '-' && index == 0 -> negative = true
                c == '.' || c == '٫' -> sb.append('.')
                c == ',' || c == '٬' || c == '،' -> sb.append(',')
                c == ' ' || c == ' ' || c == ' ' -> Unit // grouping spaces
                Character.isDigit(c) -> sb.append(Character.digit(c, 10))
                else -> return null
            }
        }
        var s = sb.toString()
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        s = when {
            lastDot >= 0 && lastComma >= 0 -> {
                val decimalAt = maxOf(lastDot, lastComma)
                s.substring(0, decimalAt).filter { it.isDigit() } + "." + s.substring(decimalAt + 1)
            }
            lastDot >= 0 && s.count { it == '.' } > 1 -> s.replace(".", "")
            lastComma >= 0 && s.count { it == ',' } > 1 -> s.replace(",", "")
            lastComma >= 0 && isGrouping(s, lastComma) -> s.replace(",", "")
            lastComma >= 0 -> s.replace(',', '.')
            else -> s
        }
        if (!VALID.matches(s)) return null
        return if (negative) "-$s" else s
    }

    private fun isGrouping(s: String, commaAt: Int): Boolean {
        val head = s.substring(0, commaAt)
        val tail = s.substring(commaAt + 1)
        return tail.length == 3 && head.isNotEmpty() && head.length <= 3 && head != "0" && tail.all { it.isDigit() }
    }

    private val VALID = Regex("""\d+(\.\d*)?|\.\d+""")

    fun parse(text: String): NumberParse {
        val normalized = normalize(text) ?: return NumberParse.Error(NumberError.INVALID)
        if (normalized.isEmpty()) return NumberParse.Error(NumberError.EMPTY)
        val negative = normalized.startsWith("-")
        val digits = normalized.removePrefix("-")
        if (!VALID.matches(digits)) return NumberParse.Error(NumberError.INVALID)
        val value = digits.toDoubleOrNull() ?: return NumberParse.Error(NumberError.INVALID)
        if (!value.isFinite()) return NumberParse.Error(NumberError.TOO_LARGE)
        if (negative && value != 0.0) return NumberParse.Error(NumberError.NEGATIVE)
        if (value > MAX_VALUE) return NumberParse.Error(NumberError.TOO_LARGE)
        return NumberParse.Ok(value)
    }

    fun parseTarget(text: String): NumberParse = when (val r = parse(text)) {
        is NumberParse.Ok -> if (r.value > 0.0) r else NumberParse.Error(NumberError.ZERO)
        is NumberParse.Error -> r
    }

    fun parseCurrent(text: String): NumberParse = parse(text)

    fun parseIncrement(text: String): NumberParse = parseTarget(text)

    fun resolveProgress(mode: ProgressMode, input: String, current: Double): NumberParse = when (mode) {
        ProgressMode.SET -> parseCurrent(input)
        ProgressMode.ADD -> when (val r = parseIncrement(input)) {
            is NumberParse.Ok -> {
                val sum = current + r.value
                if (sum.isFinite() && sum <= MAX_VALUE) NumberParse.Ok(sum) else NumberParse.Error(NumberError.TOO_LARGE)
            }
            is NumberParse.Error -> r
        }
    }

    fun format(value: Double, locale: Locale = Locale.getDefault()): String {
        if (!value.isFinite()) return "0"
        return NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(value)
    }

    fun toEditText(value: Double): String =
        if (!value.isFinite()) "0" else BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}
