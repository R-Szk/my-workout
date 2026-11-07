package com.myworkout.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WorkoutHistoryRepository(database: AppDatabase) {
    private val dao = database.workoutSessionDao()
    fun observeHistory(): Flow<List<WorkoutHistory>> = dao.observeHistory().map { sessions ->
        sessions.map { it.copy(exercises = it.exercises.sortedBy { e -> e.exercise.position }.map { e -> e.copy(sets = e.sets.sortedBy { s -> s.sequence }) }) }
    }
    suspend fun session(id: Long): WorkoutHistory? = dao.history(id)?.let { it.copy(exercises = it.exercises.sortedBy { e -> e.exercise.position }) }
    suspend fun previousSets(exercise: SessionExercise, history: List<WorkoutHistory>? = null): List<SetRecord> {
        val key = VisualCatalog.comparisonKey(exercise) ?: return emptyList()
        return (history ?: observeHistory().first()).firstNotNullOfOrNull { workout ->
            workout.exercises.filter { it.exercise.status != ExerciseStatus.SKIPPED && VisualCatalog.comparisonKey(it.exercise) == key }
                .flatMap { it.sets }.sortedWith(compareBy<SetRecord> { it.completedAt }.thenBy { it.id }).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }
}
