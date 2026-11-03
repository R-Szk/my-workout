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
    data class Ready(val routines: List<RoutineWithExercises>) : CatalogState
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
                repository.seedIfEmpty()
                combine(repository.observeRoutine(1), repository.observeRoutine(2)) { a, b ->
                    if (a != null && b != null) CatalogState.Ready(listOf(a, b)) else CatalogState.Error
                }.collect { mutableState.value = it }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableState.value = CatalogState.Error
            }
        }
    }
}
