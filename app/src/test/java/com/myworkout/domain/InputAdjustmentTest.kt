package com.myworkout.domain

import org.junit.Assert.*
import org.junit.Test

class InputAdjustmentTest {
    @Test fun weightUsesExactDecimalStepsAndNeverGoesBelowZero() {
        assertEquals("22.5", InputAdjustment.weight("20", 1))
        assertEquals("0", InputAdjustment.weight("0.5", -1))
        assertEquals("5", InputAdjustment.weight("2,5", 1))
        assertEquals("2.5", InputAdjustment.weight("invalid", 1))
    }
    @Test fun repetitionsRemainPositiveIntegersWithoutOverflow() {
        assertEquals("1", InputAdjustment.reps("1", -1))
        assertEquals("11", InputAdjustment.reps("10", 1))
        assertEquals(Int.MAX_VALUE.toString(), InputAdjustment.reps(Int.MAX_VALUE.toString(), 1))
    }
}
