package com.myworkout.domain

import org.junit.Assert.*
import org.junit.Test

class StepPlaybackTest {
    @Test fun imagesPlayForwardThenBackwardWithoutRepeatingEndpoints() {
        assertEquals(listOf(0,1,2,1,0,1,2,1,0), (0L..8L).map(StepPlayback::frameAt))
    }
    @Test fun acceptsCustomSecondsWithDefaultOneSecond() {
        assertEquals(1000L, StepPlayback.DEFAULT_INTERVAL_MILLIS)
        assertEquals(1000L, StepPlayback.parseIntervalMillis("1"))
        assertEquals(500L, StepPlayback.parseIntervalMillis("0.5"))
        assertEquals(750L, StepPlayback.parseIntervalMillis(" 0,75 "))
        assertEquals(100L, StepPlayback.parseIntervalMillis("0.1"))
        assertEquals(60000L, StepPlayback.parseIntervalMillis("60"))
        assertEquals("0.75", StepPlayback.secondsText(750L))
    }
    @Test fun rejectsInvalidOrUnsafeIntervals() {
        for (value in listOf("", "0", "-1", "0.09", "61", "NaN", "Infinity", "abc"))
            assertNull(value, StepPlayback.parseIntervalMillis(value))
    }
}
