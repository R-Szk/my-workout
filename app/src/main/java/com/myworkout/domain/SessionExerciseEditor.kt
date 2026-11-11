package com.myworkout.domain

import androidx.room.withTransaction
import com.myworkout.data.*
import kotlinx.coroutines.flow.first

class SessionExerciseEditor(private val database: AppDatabase) {
    private val dao = database.workoutSessionDao()

    suspend fun reorder(sessionId: Long, orderedSessionExerciseIds: List<Long>) = database.withTransaction {
        val session = requireActive(sessionId)
        val exercises = dao.exercises(sessionId)
        require(orderedSessionExerciseIds.size == exercises.size && orderedSessionExerciseIds.toSet() == exercises.map { it.id }.toSet()) {
            "現在の種目を重複なくすべて指定してください。"
        }
        val currentId = exercises[session.currentPosition].id
        // The unique (sessionId, position) index must hold during a swap as well.
        // Temporary negative positions are visible only inside this transaction.
        exercises.forEachIndexed { index, exercise -> dao.updatePosition(exercise.id, -index - 1) }
        orderedSessionExerciseIds.forEachIndexed { index, id -> dao.updatePosition(id, index) }
        dao.updateSession(session.copy(currentPosition = orderedSessionExerciseIds.indexOf(currentId)))
    }

    suspend fun skip(sessionExerciseId: Long) = database.withTransaction {
        val exercise = requireNotNull(dao.exercise(sessionExerciseId)) { "種目が見つかりません。" }
        val session = requireActive(exercise.sessionId)
        if (exercise.status == ExerciseStatus.SKIPPED) return@withTransaction
        require(dao.records(sessionExerciseId).isEmpty()) { "記録済みのセットを取り消してからスキップしてください。" }
        dao.updateExercise(exercise.copy(status = ExerciseStatus.SKIPPED))
        if (session.currentPosition == exercise.position) {
            val remaining = dao.exercises(session.id).filter { it.status != ExerciseStatus.SKIPPED }
            val next = remaining.firstOrNull { it.position > exercise.position }
                ?: remaining.lastOrNull { it.position < exercise.position }
            if (next != null) dao.updateSession(session.copy(currentPosition = next.position))
        }
    }

    suspend fun restore(sessionExerciseId: Long) = database.withTransaction {
        val exercise = requireNotNull(dao.exercise(sessionExerciseId)) { "種目が見つかりません。" }
        requireActive(exercise.sessionId)
        dao.updateExercise(exercise.copy(status = ExerciseStatus.PENDING))
    }

    suspend fun add(sessionId: Long, machineId: Long): Long = database.withTransaction {
        val session = requireActive(sessionId)
        val machine = requireNotNull(database.workoutDao().observeMachines().first().find { it.id == machineId }) { "マシンが見つかりません。" }
        val target = database.workoutDao().observeRoutine(session.routineId).first()?.exercises
            ?.firstOrNull { it.machine.id == machineId }?.exercise
        val exercises = dao.exercises(sessionId)
        val id = dao.insertExercise(SessionExercise(
            sessionId = sessionId, machineId = machine.id, machineName = machine.name, notes = machine.notes,
            position = exercises.size, targetSets = target?.targetSets ?: 2, targetReps = target?.targetReps ?: 10,
            visualKey = machine.visualKey ?: VisualCatalog.defaultForMachine(machine.id)?.key,
            loadType = VisualCatalog.find(machine.visualKey ?: VisualCatalog.defaultForMachine(machine.id)?.key)?.loadType,
        ))
        if (exercises.all { it.status == ExerciseStatus.SKIPPED }) {
            dao.updateSession(session.copy(currentPosition = exercises.size))
        }
        id
    }

    private suspend fun requireActive(sessionId: Long): WorkoutSession {
        val session = requireNotNull(dao.session(sessionId)) { "トレーニングが見つかりません。" }
        require(session.status == SessionStatus.ACTIVE) { "進行中のトレーニングだけ変更できます。" }
        return session
    }
}
