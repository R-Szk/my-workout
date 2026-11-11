package com.myworkout.feature.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.myworkout.data.ExerciseStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseQueueSheet(state: WorkoutUiState, model: WorkoutViewModel, onDismiss: () -> Unit) {
    val active = state.active ?: return
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var movedId by remember { mutableStateOf<Long?>(null) }
    var scrollToAdded by remember { mutableStateOf(false) }
    LaunchedEffect(active.exercises.map { it.exercise.id }) {
        val index = if (scrollToAdded) active.exercises.lastIndex
            else active.exercises.indexOfFirst { it.exercise.id == movedId }
        if (index >= 0) listState.scrollToItem(index)
        movedId = null
        scrollToAdded = false
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 650.dp).navigationBarsPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("当日の種目", style = MaterialTheme.typography.titleLarge)
            Text("変更は今回のトレーニングだけに適用します。種目名を押すと移動できます。")
            if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error)
            LazyColumn(Modifier.weight(1f, fill = false), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(active.exercises, key = { it.exercise.id }) { item ->
                    val exercise = item.exercise
                    val skipped = exercise.status == ExerciseStatus.SKIPPED
                    val description = "${exercise.position + 1}番目の${exercise.machineName}"
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "${exercise.position + 1}. ${exercise.machineName}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !state.busy, role = Role.Button) {
                                    model.selectExercise(exercise.id)
                                    onDismiss()
                                }.padding(vertical = 8.dp),
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (skipped) "スキップ（未実施）" else if (item.sets.isEmpty()) "未記録" else "記録済み：${item.sets.size}セット")
                                if (exercise.position == active.session.currentPosition) Text("表示中")
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row {
                                    TextButton(
                                        onClick = { movedId = exercise.id; model.moveExercise(exercise.id, -1) },
                                        enabled = !state.busy && exercise.position > 0,
                                        modifier = Modifier.semantics { contentDescription = "${description}を上へ" },
                                    ) { Text("↑") }
                                    TextButton(
                                        onClick = { movedId = exercise.id; model.moveExercise(exercise.id, 1) },
                                        enabled = !state.busy && exercise.position < active.exercises.lastIndex,
                                        modifier = Modifier.semantics { contentDescription = "${description}を下へ" },
                                    ) { Text("↓") }
                                }
                                TextButton(
                                    onClick = { if (skipped) model.restore(exercise.id) else model.skip(exercise.id) },
                                    enabled = !state.busy && (skipped || item.sets.isEmpty()),
                                    modifier = Modifier.semantics {
                                        contentDescription = if (skipped) "${description}のスキップを解除" else "${description}をスキップ"
                                    },
                                ) { Text(if (skipped) "スキップを解除" else "スキップ") }
                            }
                            if (item.sets.isNotEmpty()) Text("スキップするには記録済みセットを取り消してください。", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { showPicker = true }, enabled = !state.busy) { Text("種目を追加") }
                TextButton(onClick = onDismiss, enabled = !state.busy) { Text("閉じる") }
            }
        }
    }
    if (showPicker) ExercisePicker(
        machines = state.machines.filter { (it.catalogKey?.startsWith("home.") == true) == (active.session.routineKey?.startsWith("home.") == true) },
        onSelect = { machineId -> showPicker = false; scrollToAdded = true; model.addMachine(machineId) },
        onDismiss = { showPicker = false },
    )
}
