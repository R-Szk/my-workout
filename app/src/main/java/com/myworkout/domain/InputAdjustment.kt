package com.myworkout.domain

import java.math.BigDecimal

object InputAdjustment {
    fun weight(value: String, direction: Int): String {
        val base = value.trim().replace(',', '.').toBigDecimalOrNull()?.max(BigDecimal.ZERO) ?: BigDecimal.ZERO
        return (base + BigDecimal("2.5") * direction.toBigDecimal()).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()
    }
    fun reps(value: String, direction: Int): String = ((value.toLongOrNull() ?: 1L).coerceIn(1, Int.MAX_VALUE.toLong()) + direction)
        .coerceIn(1, Int.MAX_VALUE.toLong()).toString()
}
