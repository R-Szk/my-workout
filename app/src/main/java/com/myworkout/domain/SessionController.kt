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

class SessionController(private val database: AppDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = database.workoutSessionDao()

    suspend fun start(routineId: Long): Long = database.withTransaction {
        dao.unfinished()?.let { return@withTransaction it.id }
        val routine = requireNotNull(database.workoutDao().observeRoutine(routineId).first()) { "メニューが見つかりません。" }
        require(routine.exercises.isNotEmpty()) { "メニューに種目がありません。" }
        val started = now()
        val id = dao.insertSession(WorkoutSession(routineId = routineId, routineName = routine.routine.name, routineKey = routine.routine.catalogKey, startedAt = started, activeDurationMillis = 0L, activeStartedAt = started))
        dao.insertExercises(routine.exercises.sortedBy { it.exercise.position }.mapIndexed { index, item ->
            SessionExercise(sessionId = id, machineId = item.machine.id, machineName = item.machine.name,
                notes = item.machine.notes, position = index, targetSets = item.exercise.targetSets, targetReps = item.exercise.targetReps,
                visualKey = item.machine.visualKey ?: VisualCatalog.defaultForMachine(item.machine.id)?.key,
                loadType = VisualCatalog.find(item.machine.visualKey ?: VisualCatalog.defaultForMachine(item.machine.id)?.key)?.loadType)
        })
        id
    }

    // Explicit confirmation is tied to one unfinished session; failure rolls back both changes.
    suspend fun finishAndStart(expectedSessionId: Long, routineId: Long): Long = database.withTransaction {
        val active = requireNotNull(dao.unfinished()) { "中断中のトレーニングが見つかりません。" }
        require(active.id == expectedSessionId) { "中断中のトレーニングが変更されています。確認し直してください。" }
        finish(active.id)
        start(routineId)
    }

    // One requestId represents one button intent. Retrying it never creates another set.
    // A new intent may legitimately have exactly the same weight/reps as the preceding one.
    suspend fun completeSet(sessionExerciseId: Long, weightKg: Double?, reps: Int, requestId: String = UUID.randomUUID().toString()): Long = database.withTransaction {
        require(reps >= 1) { "回数は1以上の整数を入力してください。" }
        require(requestId.isNotBlank())
        val exercise = requireNotNull(dao.exercise(sessionExerciseId)) { "種目が見つかりません。" }
        requireActive(exercise.sessionId)
        validateWeight(exercise, weightKg)
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

    suspend fun updateSet(setId: Long, weightKg: Double?, reps: Int) = database.withTransaction {
        require(reps >= 1) { "回数は1以上の整数を入力してください。" }
        val record = requireNotNull(dao.record(setId)) { "セットが見つかりません。" }
        val exercise = requireNotNull(dao.exercise(record.sessionExerciseId))
        requireActive(exercise.sessionId)
        validateWeight(exercise, weightKg)
        dao.updateRecordValues(setId, weightKg, reps)
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
        dao.updateExercise(exercise.copy(visualKey = key, loadType = VisualCatalog.find(key)?.loadType))
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
        if (session.status == SessionStatus.ACTIVE)
            dao.updateSession(session.copy(status = SessionStatus.PAUSED, activeDurationMillis = elapsed(session), activeStartedAt = null))
    }

    suspend fun resume(sessionId: Long) = database.withTransaction {
        val session = requireNotNull(dao.session(sessionId))
        require(session.status != SessionStatus.COMPLETED) { "終了済みです。" }
        if (session.status == SessionStatus.PAUSED)
            dao.updateSession(session.copy(status = SessionStatus.ACTIVE, activeStartedAt = now()))
    }

    suspend fun finish(sessionId: Long): Boolean = database.withTransaction {
        val session = requireNotNull(dao.session(sessionId))
        if (session.status != SessionStatus.COMPLETED) {
            dao.updateSession(session.copy(status = SessionStatus.COMPLETED, endedAt = now(), activeSlot = null,
                activeDurationMillis = elapsed(session), activeStartedAt = null))
        }
        dao.exercises(sessionId).any { it.status != ExerciseStatus.SKIPPED && dao.records(it.id).isNotEmpty() }
    }

    private fun elapsed(session: WorkoutSession): Long? = session.activeDurationMillis?.let { accumulated ->
        accumulated + if (session.status == SessionStatus.ACTIVE && session.activeStartedAt != null)
            (now() - session.activeStartedAt).coerceAtLeast(0) else 0L
    }

    private fun validateWeight(exercise: SessionExercise, weightKg: Double?) {
        if (VisualCatalog.loadTypeFor(exercise) == LoadType.BODYWEIGHT)
            require(weightKg == null) { "自重の種目は重量なしで記録します。" }
        else require(weightKg != null && weightKg.isFinite() && weightKg > 0) { "重量は0より大きい数値を入力してください。" }
    }

    private suspend fun requireActive(sessionId: Long): WorkoutSession {
        val session = requireNotNull(dao.session(sessionId)) { "トレーニングが見つかりません。" }
        require(session.status == SessionStatus.ACTIVE) { "トレーニングを再開してください。" }
        return session
    }
}
