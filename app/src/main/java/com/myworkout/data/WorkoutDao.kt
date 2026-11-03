package com.myworkout.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM machines ORDER BY id")
    fun observeMachines(): Flow<List<Machine>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeRoutine(id: Long): Flow<RoutineWithExercises?>

    @Query("SELECT (SELECT COUNT(*) FROM routines) + (SELECT COUNT(*) FROM machines) + (SELECT COUNT(*) FROM routine_exercises)")
    suspend fun catalogRowCount(): Int

    @Insert suspend fun insertMachines(machines: List<Machine>)
    @Insert suspend fun insertRoutines(routines: List<Routine>)
    @Insert suspend fun insertExercises(exercises: List<RoutineExercise>)
}
