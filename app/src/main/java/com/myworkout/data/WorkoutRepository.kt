package com.myworkout.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(private val database: AppDatabase) {
    private val dao = database.workoutDao()

    suspend fun seedIfEmpty() = database.withTransaction {
        if (dao.catalogRowCount() == 0) {
            dao.insertMachines(SeedCatalog.machines)
            dao.insertRoutines(SeedCatalog.routines)
            dao.insertExercises(SeedCatalog.exercises)
        }
    }

    fun observeRoutine(id: Long): Flow<RoutineWithExercises?> = dao.observeRoutine(id).map { routine ->
        routine?.copy(exercises = routine.exercises.sortedBy { it.exercise.position })
    }

    fun observeMachines(): Flow<List<Machine>> = dao.observeMachines()
}
