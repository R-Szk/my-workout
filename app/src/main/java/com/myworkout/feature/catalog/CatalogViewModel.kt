package com.myworkout.feature.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myworkout.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(val routines: List<RoutineWithExercises>, val legacyRoutines: List<RoutineWithExercises> = emptyList()) : CatalogState
    data object Error : CatalogState
}

class CatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(AppDatabase.getInstance(application))
    private val mutableState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val state = mutableState.asStateFlow()

    init { load() }

    fun load() {
        if (mutableState.value !is CatalogState.Error && mutableState.value !is CatalogState.Loading) return
        mutableState.value = CatalogState.Loading
        viewModelScope.launch {
            try {
                repository.prepareHome()
                AppDatabase.getInstance(getApplication()).workoutDao().observeRoutines().collect { rows ->
                    val sorted = rows.map { it.copy(exercises = it.exercises.sortedBy { e -> e.exercise.position }) }
                    val home = sorted.filter { it.routine.catalogKey?.startsWith("home.") == true }.sortedBy { it.routine.catalogKey }
                    mutableState.value = if (home.size == 2) CatalogState.Ready(home, sorted.filter { it.routine.catalogKey == null }) else CatalogState.Error
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableState.value = CatalogState.Error
            }
        }
    }
}
