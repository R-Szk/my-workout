package com.myworkout.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

class WorkoutRepository(private val database: AppDatabase) {
    private val dao = database.workoutDao()

    suspend fun seedIfEmpty() = database.withTransaction {
        if (dao.catalogRowCount() == 0) {
            dao.insertMachines(SeedCatalog.machines)
            dao.insertRoutines(SeedCatalog.routines)
            dao.insertExercises(SeedCatalog.exercises)
        }
    }

    suspend fun prepareHome() = database.withTransaction {
        val machines = dao.observeMachines().first().toMutableList()
        var nextMachine = maxOf(1000L, machines.maxOfOrNull { it.id } ?: 0L)
        val homeMachines = ExerciseSteps.all.associate { item ->
            val existing = machines.firstOrNull { it.catalogKey == item.visualKey }
            val machine = existing ?: Machine(++nextMachine, item.name, notes = HomeCatalog.notes(item.id), visualKey = item.visualKey, catalogKey = item.visualKey).also { dao.insertMachines(listOf(it)); machines.add(it) }
            item.id to machine
        }
        val routines = dao.observeRoutines().first()
        var nextRoutine = maxOf(1000L, routines.maxOfOrNull { it.routine.id } ?: 0L)
        for ((menu, name) in listOf("A" to "A：胸・肩・二の腕", "B" to "B：背中・力こぶ・下半身")) {
            val key = "home.$menu"
            if (routines.none { it.routine.catalogKey == key }) {
                val routine = Routine(++nextRoutine, name, key)
                dao.insertRoutines(listOf(routine))
                dao.insertExercises(ExerciseSteps.all.filter { it.menu == menu }.mapIndexed { index, item ->
                    RoutineExercise(routine.id, homeMachines.getValue(item.id).id, index, 2, HomeCatalog.initialReps(item.id))
                })
            }
        }
    }

    fun observeHomeRoutines(): Flow<List<RoutineWithExercises>> = dao.observeRoutines().map { rows ->
        rows.filter { it.routine.catalogKey?.startsWith("home.") == true }.sortedBy { it.routine.catalogKey }
            .map { it.copy(exercises = it.exercises.sortedBy { e -> e.exercise.position }) }
    }

    fun observeRoutine(id: Long): Flow<RoutineWithExercises?> = dao.observeRoutine(id).map { routine ->
        routine?.copy(exercises = routine.exercises.sortedBy { it.exercise.position })
    }

    fun observeMachines(): Flow<List<Machine>> = dao.observeMachines()
}
