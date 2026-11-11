package com.myworkout.domain

import java.math.BigDecimal
import kotlin.math.roundToLong

object StepPlayback {
    const val DEFAULT_INTERVAL_MILLIS = 1000L
    private val frames = intArrayOf(0, 1, 2, 1)
    fun frameAt(tick: Long): Int { require(tick >= 0); return frames[(tick % frames.size).toInt()] }
    fun parseIntervalMillis(text: String): Long? {
        val seconds = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return if (seconds.isFinite() && seconds in .1..60.0) (seconds*1000).roundToLong() else null
    }
    fun secondsText(millis: Long): String = BigDecimal.valueOf(millis, 3).stripTrailingZeros().toPlainString()
}
