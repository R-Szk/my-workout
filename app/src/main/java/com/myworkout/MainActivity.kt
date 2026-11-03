package com.myworkout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myworkout.feature.catalog.*
import com.myworkout.feature.workout.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val model: CatalogViewModel = viewModel()
                val state by model.state.collectAsStateWithLifecycle()
                val workoutModel: WorkoutViewModel = viewModel()
                val workoutState by workoutModel.state.collectAsStateWithLifecycle()
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (workoutState.showWorkout && workoutState.active != null) {
                        Box(Modifier.safeDrawingPadding()) { WorkoutScreen(workoutState, workoutModel) }
                    } else {
                        Column(Modifier.safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("My Workout", style = MaterialTheme.typography.headlineLarge)
                            Text("A/Bメニュー", style = MaterialTheme.typography.titleLarge)
                            Text("最初の2週間は各2セットから始めましょう。")
                            when (val current = state) {
                                CatalogState.Loading -> CircularProgressIndicator()
                                CatalogState.Error -> {
                                    Text("メニューを読み込めませんでした。")
                                    Button(onClick = model::load) { Text("再読み込み") }
                                }
                                is CatalogState.Ready -> {
                                    var selectedId by rememberSaveable { mutableLongStateOf(1L) }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        current.routines.forEach { routine ->
                                            FilterChip(
                                                selected = selectedId == routine.routine.id,
                                                onClick = { selectedId = routine.routine.id },
                                                label = { Text(routine.routine.name) },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                    val routine = current.routines.first { it.routine.id == selectedId }
                                    if (workoutState.message != null) Text(workoutState.message!!)
                                    if (workoutState.error != null) {
                                        Text(workoutState.error!!, color = MaterialTheme.colorScheme.error)
                                        TextButton(onClick = workoutModel::reload, enabled = !workoutState.busy) { Text("再読み込み") }
                                    }
                                    if (workoutState.active != null) {
                                        Text("中断中：${workoutState.active!!.session.routineName}")
                                        Button(onClick = workoutModel::resume, enabled = !workoutState.busy) { Text("トレーニングを再開") }
                                    } else {
                                        Button(onClick = { workoutModel.start(selectedId) }, enabled = !workoutState.loading && !workoutState.busy && workoutState.error == null) {
                                            Text("トレーニング開始")
                                        }
                                    }
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(routine.exercises, key = { it.exercise.position }) { item ->
                                            Card(Modifier.fillMaxWidth()) {
                                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("${item.exercise.position + 1}. ${item.machine.name}", style = MaterialTheme.typography.titleMedium)
                                                    Text("${item.exercise.targetReps}回 × ${item.exercise.targetSets}セット")
                                                    if (item.machine.notes.isNotBlank()) Text(item.machine.notes)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
