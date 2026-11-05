package com.myworkout.domain

import androidx.room.withTransaction
import com.myworkout.data.*
import kotlinx.coroutines.flow.first
import java.util.UUID

data class ActiveExercise(
    val exercise: SessionExercise,
    val sets: List<SetRecord>,
    val previous: SetRecord?,
)

data class ActiveWorkout(val session: WorkoutSession, val exercises: List<ActiveExercise>)

class SessionController(private val database: AppDatabase) {
    private val dao = database.workoutSessionDao()

    suspend fun start(routineId: Long): Long = database.withTransaction {
        dao.unfinished()?.let { return@withTransaction it.id }
        val routine = requireNotNull(database.workoutDao().observeRoutine(routineId).first()) { "メニューが見つかりません。" }
        require(routine.exercises.isNotEmpty()) { "メニューに種目がありません。" }
        val id = dao.insertSession(WorkoutSession(routineId = routineId, routineName = routine.routine.name, startedAt = System.currentTimeMillis()))
        dao.insertExercises(routine.exercises.sortedBy { it.exercise.position }.mapIndexed { index, item ->
            SessionExercise(sessionId = id, machineId = item.machine.id, machineName = item.machine.name,
                notes = item.machine.notes, position = index, targetSets = item.exercise.targetSets, targetReps = item.exercise.targetReps)
        })
        id
    }

    // One requestId represents one button intent. Retrying it never creates another set.
    // A new intent may legitimately have exactly the same weight/reps as the preceding one.
    suspend fun completeSet(sessionExerciseId: Long, weightKg: Double, reps: Int, requestId: String = UUID.randomUUID().toString()): Long = database.withTransaction {
        require(weightKg.isFinite() && weightKg >= 0) { "重量は0以上の数値を入力してください。" }
        require(reps >= 1) { "回数は1以上の整数を入力してください。" }
        require(requestId.isNotBlank())
        val exercise = requireNotNull(dao.exercise(sessionExerciseId)) { "種目が見つかりません。" }
        requireActive(exercise.sessionId)
        require(exercise.status != ExerciseStatus.SKIPPED) { "スキップを解除してから記録してください。" }
        dao.recordForRequest(requestId)?.let { existing ->
            require(existing.sessionExerciseId == sessionExerciseId && existing.weightKg == weightKg && existing.reps == reps) { "保存要求が一致しません。" }
            return@withTransaction existing.id
        }
        val sequence = (dao.records(sessionExerciseId).lastOrNull()?.sequence ?: 0) + 1
        dao.insertRecord(SetRecord(sessionExerciseId = sessionExerciseId, sequence = sequence, weightKg = weightKg,
            reps = reps, completedAt = System.currentTimeMillis(), requestId = requestId))
    }

    suspend fun undoSet(setId: Long) = database.withTransaction {
        val record = dao.record(setId) ?: return@withTransaction
        val exercise = requireNotNull(dao.exercise(record.sessionExerciseId))
        requireActive(exercise.sessionId)
        dao.deleteRecord(setId)
    }

    suspend fun resumeActive(): ActiveWorkout? = database.withTransaction {
        val session = dao.unfinished() ?: return@withTransaction null
        ActiveWorkout(session, dao.exercises(session.id).map { exercise ->
            ActiveExercise(exercise, dao.records(exercise.id), dao.previousRecord(exercise.machineId))
        })
    }

    suspend fun moveTo(sessionId: Long, position: Int) = database.withTransaction {
        val session = requireActive(sessionId)
        require(position in dao.exercises(sessionId).indices) { "種目の位置が不正です。" }
        dao.updateSession(session.copy(currentPosition = position))
    }

    suspend fun pause(sessionId: Long) = database.withTransaction {
        val session = requireNotNull(dao.session(sessionId))
        require(session.status != SessionStatus.COMPLETED) { "終了済みです。" }
        dao.updateSession(session.copy(status = SessionStatus.PAUSED))
    }

    suspend fun resume(sessionId: Long) = database.withTransaction {
        val session = requireNotNull(dao.session(sessionId))
        require(session.status != SessionStatus.COMPLETED) { "終了済みです。" }
        dao.updateSession(session.copy(status = SessionStatus.ACTIVE))
    }

    suspend fun finish(sessionId: Long) = database.withTransaction {
        val session = requireNotNull(dao.session(sessionId))
        if (session.status != SessionStatus.COMPLETED) {
            dao.updateSession(session.copy(status = SessionStatus.COMPLETED, endedAt = System.currentTimeMillis(), activeSlot = null))
        }
    }

    private suspend fun requireActive(sessionId: Long): WorkoutSession {
        val session = requireNotNull(dao.session(sessionId)) { "トレーニングが見つかりません。" }
        require(session.status == SessionStatus.ACTIVE) { "トレーニングを再開してください。" }
        return session
    }
}
