package com.myworkout.domain

import com.myworkout.data.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ProgressCalculatorTest {
    private val zone = ZoneId.of("Asia/Tokyo")
    private fun workout(id: Long, date: String, weights: List<Pair<Double, Int>>, machine: Long = 1, key: String? = "chest_press"): WorkoutHistory {
        val start = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()
        return WorkoutHistory(WorkoutSession(id, 1, "A", start, start + 120000, SessionStatus.COMPLETED, 0, null),
            listOf(ExerciseHistory(SessionExercise(id, id, machine, "種目", "", 0, 3, 10, visualKey = key),
                weights.mapIndexed { i, pair -> SetRecord(id * 10 + i, id, i + 1, pair.first, pair.second, start + i, "$id-$i") })))
    }
    @Test fun countsOnlyRealSetsAndCalendarRespectsLocalMonth() {
        val old = workout(1, "2026-10-31", listOf(20.0 to 8))
        val current = workout(2, "2026-11-01", listOf(20.0 to 10, 20.0 to 9))
        val empty = workout(3, "2026-11-02", emptyList())
        val summary = ProgressCalculator.summary(current, listOf(old, current, empty), zone)
        assertEquals(1, summary.exerciseCount)
        assertEquals(2, summary.setCount)
        assertEquals(120000L, summary.durationMillis)
        assertEquals(setOf(LocalDate.parse("2026-11-01")), summary.trainingDays)
        assertEquals(2, summary.progress.single().repetitionChange)
        assertEquals(0.0, summary.progress.single().weightChange!!, 0.0)
        assertEquals(MuscleRole.PRIMARY, summary.muscles[Muscle.CHEST])
    }
    @Test fun differentWeightsNeverBecomeRepetitionImprovementAndBestKeepsItsReps() {
        val old = workout(1, "2026-11-01", listOf(20.0 to 12))
        val now = workout(2, "2026-11-02", listOf(20.0 to 15, 25.0 to 7))
        val progress = ProgressCalculator.summary(now, listOf(old, now), zone).progress.single()
        assertNull(progress.repetitionChange)
        assertEquals(5.0, progress.weightChange!!, 0.0)
        assertEquals(listOf(12, 7), progress.points.map { it.reps })
    }
    @Test fun assistanceUsesLessAssistanceAndDifferentVariantsAreNotCompared() {
        val old = workout(1, "2026-11-01", listOf(40.0 to 10), 5, "assisted_dip")
        val now = workout(2, "2026-11-02", listOf(35.0 to 8, 30.0 to 6), 5, "assisted_dip")
        val other = workout(3, "2026-11-03", listOf(10.0 to 10), 5, "assisted_chin")
        val result = ProgressCalculator.summary(now, listOf(old, now, other), zone).progress.single()
        assertEquals(-10.0, result.weightChange!!, 0.0)
        assertEquals(LoadType.ASSISTANCE, result.loadType)
        assertEquals(30.0, result.points.last().weightKg, 0.0)
        assertNull(ProgressCalculator.summary(other, listOf(old, now, other), zone).progress.single().weightChange)
    }
    @Test fun sameWeightGraphKeepsSetsHiddenByHeavierDailyBest() {
        val old = workout(1, "2026-11-01", listOf(20.0 to 10, 25.0 to 8))
        val earlierToday = workout(2, "2026-11-02", listOf(25.0 to 9))
        val current = workout(3, "2026-11-02", listOf(20.0 to 12))
        val result = ProgressCalculator.summary(current,listOf(old,earlierToday,current),zone).progress.single()
        assertEquals(listOf(10,12),result.repetitionPoints.map { it.reps })
        assertEquals(listOf(20.0,20.0),result.repetitionPoints.map { it.weightKg })
    }

    @Test fun comparisonUsesSameWeightSetEvenIfPreviousSessionHadHeavierSets() {
        val old = workout(1,"2026-11-01",listOf(20.0 to 10,25.0 to 8))
        val current = workout(2,"2026-11-02",listOf(20.0 to 12))
        val result=ProgressCalculator.summary(current,listOf(old,current),zone).progress.single()
        assertEquals(2,result.repetitionChange)
        assertEquals(-5.0,result.weightChange!!,0.0)
    }

    @Test fun emptyAndUnknownSessionsDoNotInventProgressOrMuscles() {
        val unknown = workout(1, "2026-11-01", listOf(20.0 to 10), 5, null)
        val summary = ProgressCalculator.summary(unknown, listOf(unknown), zone)
        assertTrue(summary.muscles.isEmpty())
        assertEquals(1, summary.unknownExercises)
        assertTrue(summary.progress.isEmpty())
        val empty = workout(2, "2026-11-02", emptyList())
        val zero = ProgressCalculator.summary(empty, listOf(empty), zone)
        assertEquals(0, zero.setCount)
        assertTrue(zero.trainingDays.isEmpty())
    }
}
