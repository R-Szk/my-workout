package com.myworkout.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE activeSlot = 1 LIMIT 1")
    suspend fun unfinished(): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun session(id: Long): WorkoutSession?

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    suspend fun exercises(sessionId: Long): List<SessionExercise>

    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun exercise(id: Long): SessionExercise?

    @Query("SELECT * FROM set_records WHERE sessionExerciseId = :exerciseId ORDER BY sequence")
    suspend fun records(exerciseId: Long): List<SetRecord>

    @Query("SELECT * FROM set_records WHERE id = :id")
    suspend fun record(id: Long): SetRecord?

    @Query("SELECT * FROM set_records WHERE requestId = :requestId LIMIT 1")
    suspend fun recordForRequest(requestId: String): SetRecord?

    @Query("""SELECT r.* FROM set_records r
        INNER JOIN session_exercises e ON e.id = r.sessionExerciseId
        INNER JOIN workout_sessions s ON s.id = e.sessionId
        WHERE e.machineId = :machineId AND s.status = 'COMPLETED' AND e.status <> 'SKIPPED'
        ORDER BY s.endedAt DESC, s.id DESC, r.completedAt DESC, r.id DESC LIMIT 1""")
    suspend fun previousRecord(machineId: Long): SetRecord?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY startedAt DESC, id DESC")
    fun observeHistory(): Flow<List<WorkoutHistory>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun history(id: Long): WorkoutHistory?

    @Query("UPDATE machines SET visualKey = :visualKey WHERE id = :id")
    suspend fun updateMachineVisual(id: Long, visualKey: String)

    @Insert suspend fun insertSession(session: WorkoutSession): Long
    @Insert suspend fun insertExercises(exercises: List<SessionExercise>)
    @Insert suspend fun insertExercise(exercise: SessionExercise): Long
    @Update suspend fun updateExercise(exercise: SessionExercise)
    @Query("UPDATE session_exercises SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)
    @Insert suspend fun insertRecord(record: SetRecord): Long
    @Update suspend fun updateSession(session: WorkoutSession)
    @Query("DELETE FROM set_records WHERE id = :id") suspend fun deleteRecord(id: Long)
}
