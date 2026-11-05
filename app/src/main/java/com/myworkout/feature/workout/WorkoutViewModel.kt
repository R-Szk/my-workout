package com.myworkout.feature.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val controller = SessionController(database)
    private val editor = SessionExerciseEditor(database)
    private val mutableState = MutableStateFlow(WorkoutUiState())
    val state = mutableState.asStateFlow()

    init { reload() }

    fun reload() = perform {
        val repository = WorkoutRepository(database)
        repository.seedIfEmpty()
        mutableState.value = mutableState.value.copy(machines = repository.observeMachines().first())
    }

    fun start(routineId: Long) = perform {
        val id = controller.start(routineId)
        controller.resume(id)
        mutableState.value = mutableState.value.copy(showWorkout = true, message = null)
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

    fun completeSet(exerciseId: Long, weight: String, reps: String, requestId: String) {
        if (mutableState.value.busy) return
        val parsedWeight = weight.trim().replace(',', '.').toDoubleOrNull()
        val parsedReps = reps.trim().toIntOrNull()
        if (parsedWeight == null || !parsedWeight.isFinite() || parsedWeight < 0 || parsedReps == null || parsedReps < 1) {
            mutableState.value = mutableState.value.copy(error = "重量は0以上の数値、回数は1以上の整数を入力してください。")
            return
        }
        perform { controller.completeSet(exerciseId, parsedWeight, parsedReps, requestId) }
    }

    fun undo(setId: Long) = perform { controller.undoSet(setId) }

    fun finish() = perform {
        controller.finish(requireNotNull(mutableState.value.active).session.id)
        mutableState.value = mutableState.value.copy(showWorkout = false, message = "トレーニングを保存して終了しました。")
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
