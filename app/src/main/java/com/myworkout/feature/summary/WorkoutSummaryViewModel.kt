package com.myworkout.feature.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myworkout.data.*
import com.myworkout.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class SummaryUiState(val loading: Boolean = true, val summary: WorkoutSummary? = null, val history: List<WorkoutHistory> = emptyList(), val error: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutHistoryRepository(AppDatabase.getInstance(application))
    private val selected = MutableStateFlow<Long?>(null)
    private val refresh = MutableStateFlow(0)
    val state = refresh.flatMapLatest {
        combine(selected, repository.observeHistory()) { id, history ->
            SummaryUiState(false, history.find { it.session.id == id }?.let { ProgressCalculator.summary(it,history) }, history)
        }.catch { emit(SummaryUiState(loading = false, error = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SummaryUiState())
    fun select(id: Long?) { selected.value = id }
    fun retry() { refresh.value += 1 }
}
