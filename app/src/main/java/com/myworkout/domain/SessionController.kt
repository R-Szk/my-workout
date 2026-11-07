package com.myworkout.domain

import androidx.room.withTransaction
import com.myworkout.data.*
import kotlinx.coroutines.flow.first
import java.util.UUID

data class ActiveExercise(
    val exercise: SessionExercise,
    val sets: List<SetRecord>,
    val previous: SetRecord?,
    val previousSets: List<SetRecord> = emptyList(),
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
                notes = item.machine.notes, position = index, targetSets = item.exercise.targetSets, targetReps = item.exercise.targetReps,
                visualKey = item.machine.visualKey ?: VisualCatalog.defaultForMachine(item.machine.id)?.key)
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
        val existingRecords = dao.records(sessionExerciseId)
        val legacyUnclassified = exercise.visualKey == null && existingRecords.isNotEmpty()
        require(VisualCatalog.options(exercise.machineId).isEmpty() || VisualCatalog.resolve(exercise) != null || legacyUnclassified) { "機種・種目を選択してから記録してください。" }
        require(exercise.status != ExerciseStatus.SKIPPED) { "スキップを解除してから記録してください。" }
        dao.recordForRequest(requestId)?.let { existing ->
            require(existing.sessionExerciseId == sessionExerciseId && existing.weightKg == weightKg && existing.reps == reps) { "保存要求が一致しません。" }
            return@withTransaction existing.id
        }
        val sequence = (existingRecords.lastOrNull()?.sequence ?: 0) + 1
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
        val repository = WorkoutHistoryRepository(database)
        val history = repository.observeHistory().first()
        ActiveWorkout(session, dao.exercises(session.id).map { exercise ->
            val previous = repository.previousSets(exercise, history)
            ActiveExercise(exercise, dao.records(exercise.id), previous.lastOrNull(), previous)
        })
    }

    suspend fun chooseVisual(exerciseId: Long, key: String) = database.withTransaction {
        val exercise = requireNotNull(dao.exercise(exerciseId))
        requireActive(exercise.sessionId)
        require(dao.records(exerciseId).isEmpty()) { "記録済みの種目は変更できません。" }
        require(VisualCatalog.options(exercise.machineId).any { it.key == key }) { "対応していない機種・種目です。" }
        dao.updateExercise(exercise.copy(visualKey = key))
        dao.updateMachineVisual(exercise.machineId, key)
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
