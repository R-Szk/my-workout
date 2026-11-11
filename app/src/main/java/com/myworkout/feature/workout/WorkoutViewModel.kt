package com.myworkout.feature.workout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myworkout.data.*
import com.myworkout.data.Machine
import com.myworkout.data.AppDatabase
import com.myworkout.data.WorkoutRepository
import com.myworkout.domain.ActiveWorkout
import com.myworkout.domain.SessionController
import com.myworkout.domain.SessionExerciseEditor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WorkoutUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val active: ActiveWorkout? = null,
    val showWorkout: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val machines: List<Machine> = emptyList(),
    val summaryId: Long? = null,
)

class WorkoutViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val controller = SessionController(database)
    private val editor = SessionExerciseEditor(database)
    private val mutableState = MutableStateFlow(WorkoutUiState(summaryId = savedState["summaryId"]))
    private var lastSaveIntent = -1000L
    val state = mutableState.asStateFlow()

    init { reload() }

    fun reload() = perform {
        val repository = WorkoutRepository(database)
        repository.prepareHome()
        mutableState.value = mutableState.value.copy(machines = repository.observeMachines().first())
    }

    fun start(routineId: Long) = perform {
        val id = controller.start(routineId)
        controller.resume(id)
        mutableState.value = mutableState.value.copy(showWorkout = true, message = null)
    }

    fun switchRoutine(expectedSessionId: Long, routineId: Long) = perform {
        controller.finishAndStart(expectedSessionId, routineId)
        savedState["summaryId"] = null
        mutableState.value = mutableState.value.copy(showWorkout = true, summaryId = null, message = null)
    }

    fun resume() = perform {
        controller.resume(requireNotNull(mutableState.value.active).session.id)
        mutableState.value = mutableState.value.copy(showWorkout = true, message = null)
    }

    fun pause() = perform {
        controller.pause(requireNotNull(mutableState.value.active).session.id)
        mutableState.value = mutableState.value.copy(showWorkout = false)
    }

    fun move(delta: Int) = perform {
        val active = requireNotNull(mutableState.value.active)
        controller.moveTo(active.session.id, active.session.currentPosition + delta)
    }

    fun moveExercise(exerciseId: Long, delta: Int) = perform {
        val active = requireNotNull(mutableState.value.active)
        val ids = active.exercises.map { it.exercise.id }.toMutableList()
        val from = ids.indexOf(exerciseId)
        val to = from + delta
        require(from in ids.indices && to in ids.indices)
        val moved = ids.removeAt(from)
        ids.add(to, moved)
        editor.reorder(active.session.id, ids)
    }

    fun skip(exerciseId: Long) = perform { editor.skip(exerciseId) }
    fun restore(exerciseId: Long) = perform { editor.restore(exerciseId) }
    fun addMachine(machineId: Long) = perform {
        editor.add(requireNotNull(mutableState.value.active).session.id, machineId)
    }

    fun selectExercise(exerciseId: Long) = perform {
        val active = requireNotNull(mutableState.value.active)
        val index = active.exercises.indexOfFirst { it.exercise.id == exerciseId }
        controller.moveTo(active.session.id, index)
    }

    fun chooseVisual(exerciseId: Long, key: String) = perform { controller.chooseVisual(exerciseId, key) }
    fun showSummary(id: Long) { savedState["summaryId"] = id; mutableState.value = mutableState.value.copy(summaryId = id) }
    fun deleteWorkoutHistory(id: Long, onDeleted: () -> Unit) = perform {
        require(WorkoutHistoryRepository(database).deleteCompleted(id)) { "記録を削除できませんでした。履歴を確認してください。" }
        if (mutableState.value.summaryId == id) {
            savedState["summaryId"] = null
            mutableState.value = mutableState.value.copy(summaryId = null)
        }
        mutableState.value = mutableState.value.copy(message = "ワークアウトを削除しました。")
        onDeleted()
    }

    fun closeSummary() { savedState["summaryId"] = null; mutableState.value = mutableState.value.copy(summaryId = null) }

    fun completeSet(exerciseId: Long, weight: String, reps: String, requestId: String) {
        if (mutableState.value.busy) return
        val exercise = mutableState.value.active?.exercises?.find { it.exercise.id == exerciseId }?.exercise ?: return
        val parsed = try { parseInput(exercise, weight, reps) } catch (error: IllegalArgumentException) {
            mutableState.value = mutableState.value.copy(error = error.message); return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastSaveIntent < 600) return
        lastSaveIntent = now
        perform { controller.completeSet(exerciseId, parsed.first, parsed.second, requestId) }
    }

    fun clearError() { mutableState.value = mutableState.value.copy(error = null) }

    fun updateSet(setId: Long, weight: String, reps: String, onSaved: () -> Unit) = perform {
        val exercise = requireNotNull(mutableState.value.active?.exercises?.find { item -> item.sets.any { it.id == setId } }?.exercise)
        val parsed = parseInput(exercise, weight, reps)
        controller.updateSet(setId, parsed.first, parsed.second)
        onSaved()
    }

    fun undo(setId: Long) = perform { controller.undoSet(setId) }

    fun finish() = perform {
        val id = requireNotNull(mutableState.value.active).session.id
        val recorded = controller.finish(id)
        val summaryId = id.takeIf { recorded }
        savedState["summaryId"] = summaryId
        mutableState.value = mutableState.value.copy(showWorkout = false, active = null, summaryId = summaryId,
            message = if(recorded) "トレーニングを保存して終了しました。" else "セットが未記録のため、記録を残さず終了しました。")
    }

    private fun parseInput(exercise: SessionExercise, weight: String, reps: String): Pair<Double?,Int> {
        val bodyweight = VisualCatalog.loadTypeFor(exercise) == LoadType.BODYWEIGHT
        val count = reps.trim().toIntOrNull()
        if (bodyweight) {
            require(count != null && count >= 1) { "回数は1以上の整数を入力してください。" }
            return null to count
        }
        val kg = weight.trim().replace(',', '.').toDoubleOrNull()
        require(kg != null && kg.isFinite() && kg > 0 && count != null && count >= 1) {
            "重量は0より大きい数値、回数は1以上の整数を入力してください。"
        }
        return kg to count
    }

    fun startHomeExercise(item: ExerciseStepSet) = perform {
        val old = controller.resumeActive()
        if (old != null) controller.resume(old.session.id)
        else {
            val repo = WorkoutRepository(database)
            repo.prepareHome()
            val routine = repo.observeHomeRoutines().first().first { it.routine.catalogKey == "home.${item.menu}" }
            val id = controller.start(routine.routine.id)
            controller.moveTo(id, routine.exercises.indexOfFirst { it.machine.visualKey == item.visualKey })
        }
        mutableState.value = mutableState.value.copy(showWorkout = true, message = null)
    }

    private fun perform(action: suspend () -> Unit) {
        // Set synchronously before launching so two callbacks in the same frame cannot queue writes.
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                action()
                mutableState.value = mutableState.value.copy(active = controller.resumeActive())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IllegalArgumentException) {
                mutableState.value = mutableState.value.copy(error = exception.message ?: "入力内容を確認してください。")
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(error = "保存・読み込みに失敗しました。再度操作してください。")
            } finally {
                mutableState.value = mutableState.value.copy(loading = false, busy = false)
            }
        }
    }
}
