package com.myworkout.feature.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID
import com.myworkout.data.ExerciseStatus

@Composable
fun WorkoutScreen(state: WorkoutUiState, model: WorkoutViewModel) {
    val active = state.active ?: return
    val current = active.exercises[active.session.currentPosition]
    val exercise = current.exercise
    val skipped = exercise.status == ExerciseStatus.SKIPPED
    var showQueue by rememberSaveable { mutableStateOf(false) }
    val initial = current.sets.lastOrNull() ?: current.previous
    var weight by rememberSaveable(exercise.id) { mutableStateOf(initial?.weightKg?.toString() ?: "0") }
    var reps by rememberSaveable(exercise.id) { mutableStateOf((initial?.reps ?: exercise.targetReps).toString()) }
    // An old callback keeps its old ID even if it arrives after a successful save.
    val requestId = rememberSaveable(exercise.id, current.sets.lastOrNull()?.id, current.sets.size) { UUID.randomUUID().toString() }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    BackHandler(enabled = !state.busy) { model.pause() }
    val scroll = rememberScrollState()
    LaunchedEffect(exercise.id) { scroll.scrollTo(0) }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(scroll).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(active.session.routineName, style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { keyboard?.hide(); focus.clearFocus(); showQueue = true }, enabled = !state.busy) { Text("当日の種目を編集") }
        Text("${active.session.currentPosition + 1} / ${active.exercises.size}種目")
        Text(exercise.machineName, style = MaterialTheme.typography.headlineMedium)
        Text("目標：${exercise.targetReps}回 × ${exercise.targetSets}セット")
        if (skipped) {
            Text("スキップ（未実施）", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { model.restore(exercise.id) }, enabled = !state.busy) { Text("スキップを解除") }
        }
        if (exercise.notes.isNotBlank()) Text(exercise.notes)
        val previous = current.previous
        Text(if (previous == null) "前回の記録はありません。" else "前回：${previous.weightKg} kg × ${previous.reps}回")
        Text("前回値は提案です。セット完了で今回の実績を保存します。", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = weight, onValueChange = { weight = it }, label = { Text("重量 (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, enabled = !state.busy && !skipped, modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = reps, onValueChange = { reps = it }, label = { Text("回数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, enabled = !state.busy && !skipped, modifier = Modifier.weight(1f),
            )
        }
        if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            keyboard?.hide()
            focus.clearFocus()
            model.completeSet(exercise.id, weight, reps, requestId)
        }, enabled = !state.busy && !skipped, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(if (state.busy) "処理中…" else "セット完了")
        }
        Text("記録済み：${current.sets.size}セット", style = MaterialTheme.typography.titleMedium)
        current.sets.forEachIndexed { index, set ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${index + 1}セット目")
                    Text("${set.weightKg} kg × ${set.reps}回")
                }
                TextButton(onClick = { model.undo(set.id) }, enabled = !state.busy) { Text("取り消し") }
            }
        }
        Text("訂正する場合は取り消して入力し直してください。", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { model.move(-1) }, enabled = !state.busy && active.session.currentPosition > 0) { Text("前の種目") }
            OutlinedButton(onClick = { model.move(1) }, enabled = !state.busy && active.session.currentPosition < active.exercises.lastIndex) { Text("次の種目") }
        }
        OutlinedButton(onClick = model::pause, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("中断して戻る") }
        TextButton(onClick = { confirmFinish = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("トレーニングを終了") }
    }
    if (showQueue) ExerciseQueueSheet(state, model, onDismiss = { showQueue = false })
    if (confirmFinish) AlertDialog(
        onDismissRequest = { confirmFinish = false },
        title = { Text("トレーニングを終了しますか？") },
        text = { Text("完了したセットを保存して終了します。未実施のセットは記録されません。") },
        confirmButton = { TextButton(onClick = { confirmFinish = false; model.finish() }) { Text("終了する") } },
        dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("続ける") } },
    )
}
