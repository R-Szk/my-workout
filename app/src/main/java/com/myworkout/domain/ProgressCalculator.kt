package com.myworkout.domain

import com.myworkout.data.*
import java.time.*

data class PerformancePoint(val date: LocalDate, val weightKg: Double, val reps: Int)
data class ExerciseProgress(
    val key: String, val name: String, val loadType: LoadType, val points: List<PerformancePoint>,
    val weightChange: Double?, val repetitionChange: Int?,
    val currentWeightKg: Double, val currentReps: Int, val repetitionPoints: List<PerformancePoint>,
)
data class WorkoutSummary(
    val history: WorkoutHistory, val exerciseCount: Int, val setCount: Int, val durationMillis: Long,
    val progress: List<ExerciseProgress>, val muscles: Map<Muscle, MuscleRole>,
    val unknownExercises: Int, val trainingDays: Set<LocalDate>, val trainingCount: Int,
)

object ProgressCalculator {
    private fun performed(workout: WorkoutHistory) = workout.exercises.filter { it.exercise.status != ExerciseStatus.SKIPPED && it.sets.isNotEmpty() }
    fun trainingDays(history: List<WorkoutHistory>, zone: ZoneId = ZoneId.systemDefault()): Set<LocalDate> = history
        .filter { it.session.status == SessionStatus.COMPLETED && performed(it).isNotEmpty() }
        .map { Instant.ofEpochMilli(it.session.startedAt).atZone(zone).toLocalDate() }.toSet()

    fun summary(current: WorkoutHistory, history: List<WorkoutHistory>, zone: ZoneId = ZoneId.systemDefault()): WorkoutSummary {
        val exercises = performed(current)
        val date = Instant.ofEpochMilli(current.session.startedAt).atZone(zone).toLocalDate()
        val eligible = history.filter { it.session.status == SessionStatus.COMPLETED &&
            (it.session.startedAt < current.session.startedAt || (it.session.startedAt == current.session.startedAt && it.session.id <= current.session.id)) }
        val progress = exercises.groupBy { VisualCatalog.comparisonKey(it.exercise) }.mapNotNull { (key, group) ->
            if (key == null) return@mapNotNull null
            val profile = VisualCatalog.resolve(group.first().exercise) ?: return@mapNotNull null
            fun matching(workout: WorkoutHistory) = performed(workout).filter { VisualCatalog.comparisonKey(it.exercise) == key }.flatMap { it.sets }
            fun best(sets: List<SetRecord>): SetRecord? = if (profile.loadType == LoadType.ASSISTANCE)
                sets.minWithOrNull(compareBy<SetRecord> { it.weightKg }.thenByDescending { it.reps })
            else sets.maxWithOrNull(compareBy<SetRecord> { it.weightKg }.thenBy { it.reps })
            val currentBest = best(group.flatMap { it.sets })!!
            val previousSets = eligible.filter { it.session.id != current.session.id }.sortedWith(compareByDescending<WorkoutHistory> { it.session.startedAt }.thenByDescending { it.session.id })
                .firstNotNullOfOrNull { matching(it).takeIf { sets -> sets.isNotEmpty() } } ?: emptyList()
            val previous = best(previousSets)
            val previousAtWeight = previousSets.filter { it.weightKg == currentBest.weightKg }.maxByOrNull { it.reps }
            val daily = eligible.groupBy { Instant.ofEpochMilli(it.session.startedAt).atZone(zone).toLocalDate() }
                .mapNotNull { (day, sessions) -> best(sessions.flatMap { matching(it) })?.let { PerformancePoint(day, it.weightKg, it.reps) } }
                .sortedBy { it.date }
            val repetitions = eligible.groupBy { Instant.ofEpochMilli(it.session.startedAt).atZone(zone).toLocalDate() }
                .mapNotNull { (day, sessions) -> sessions.flatMap { matching(it) }.filter { it.weightKg == currentBest.weightKg }
                    .maxByOrNull { it.reps }?.let { PerformancePoint(day,it.weightKg,it.reps) } }.sortedBy { it.date }
            ExerciseProgress(key, profile.label, profile.loadType, daily,
                previous?.let { currentBest.weightKg - it.weightKg },
                previousAtWeight?.let { currentBest.reps - it.reps },
                currentBest.weightKg, currentBest.reps, repetitions)
        }
        return WorkoutSummary(
            current, exercises.size, exercises.sumOf { it.sets.size },
            ((current.session.endedAt ?: current.session.startedAt) - current.session.startedAt).coerceAtLeast(0),
            progress, MuscleCatalog.combine(exercises.mapNotNull { VisualCatalog.resolve(it.exercise) }),
            exercises.count { VisualCatalog.resolve(it.exercise) == null },
            trainingDays(history, zone).filter { YearMonth.from(it) == YearMonth.from(date) }.toSet(),
            history.count { it.session.status == SessionStatus.COMPLETED && performed(it).isNotEmpty() &&
                YearMonth.from(Instant.ofEpochMilli(it.session.startedAt).atZone(zone)) == YearMonth.from(date) },
        )
    }
}
