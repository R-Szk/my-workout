package com.myworkout.feature.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myworkout.data.*
import com.myworkout.ui.components.*
import java.util.UUID

@Composable
fun WorkoutScreen(state: WorkoutUiState, model: WorkoutViewModel) {
    val active = state.active ?: return
    val current = active.exercises[active.session.currentPosition]
    val exercise = current.exercise
    val stepImages = ExerciseSteps.find(exercise.visualKey)
    val profile = VisualCatalog.resolve(exercise)
    val bodyweight = VisualCatalog.loadTypeFor(exercise) == LoadType.BODYWEIGHT
    val options = VisualCatalog.options(exercise.machineId)
    val skipped = exercise.status == ExerciseStatus.SKIPPED
    val selectable = options.size > 1 && current.sets.isEmpty()
    val canSave = !state.busy && !skipped && (profile != null || options.isEmpty() || (exercise.visualKey == null && current.sets.isNotEmpty()))
    val saveLabel = if (state.busy) "処理中…" else "${current.sets.size + 1}セット完了"
    val initial = current.sets.lastOrNull() ?: current.previous
    var weight by rememberSaveable(exercise.id, exercise.visualKey) { mutableStateOf(initial?.weightKg?.toString() ?: "0") }
    var reps by rememberSaveable(exercise.id, exercise.visualKey) { mutableStateOf((initial?.reps ?: exercise.targetReps).toString()) }
    val requestId = rememberSaveable(exercise.id, exercise.visualKey, current.sets.lastOrNull()?.id, current.sets.size) { UUID.randomUUID().toString() }
    var editingSetId by rememberSaveable(exercise.id) { mutableStateOf<Long?>(null) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showChoice by rememberSaveable { mutableStateOf(false) }
    var showPrevious by remember { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val scroll = rememberScrollState()
    LaunchedEffect(exercise.id) { scroll.scrollTo(0) }
    BackHandler { if (!state.busy) model.pause() }
    BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
        val tiny = maxHeight < 350.dp
        val compact = maxHeight < 600.dp
        val heroHeight = if (tiny) (maxHeight * .22f).coerceIn(32.dp,64.dp) else if (compact) 76.dp else if (maxHeight < 660.dp) 120.dp else 170.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(if(tiny) 4.dp else 8.dp)) {
            if(tiny) Text("${active.session.routineName.substringBefore('：')} · ${active.session.currentPosition+1}/${active.exercises.size} · 記録${current.sets.size}セット · ${exercise.machineName}",
                style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
            else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(active.session.routineName, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = { keyboard?.hide();focus.clearFocus();showQueue = true }, enabled = !state.busy) { Text("当日の種目を編集") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(exercise.machineName, Modifier.weight(1f), style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
                Column {
                    Text("${active.session.currentPosition+1} / ${active.exercises.size}種目", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("記録済み：${current.sets.size}セット",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                }
            }
            LinearProgressIndicator(progress = { (active.session.currentPosition+1).toFloat()/active.exercises.size }, modifier = Modifier.fillMaxWidth().height(3.dp))
            }
            Row(Modifier.fillMaxWidth().height(heroHeight), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (stepImages != null) ExerciseStepImages(stepImages, Modifier.weight(2.5f).fillMaxHeight())
                else MachinePhoto(profile?.machineImage, exercise.machineName, Modifier.weight(1f).fillMaxHeight())
                if (stepImages != null) Card(Modifier.weight(1f).fillMaxHeight()) {
                    BodyMap(stepImages.muscles.associateWith { MuscleRole.PRIMARY }, Modifier.fillMaxSize(), primaryColor = androidx.compose.ui.graphics.Color(0xFF2DD4BF))
                } else MuscleMap(profile, Modifier.weight(1f).fillMaxHeight())
            }
            Column(Modifier.weight(1f).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if(tiny) TextButton(onClick={keyboard?.hide();focus.clearFocus();showQueue=true},enabled=!state.busy) { Text("当日の種目を編集") }
                if (options.size > 1) Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(profile?.label ?: "機種・種目が未選択です", style = MaterialTheme.typography.titleSmall)
                        if (profile == null && current.sets.isNotEmpty()) Text("既存記録は未分類のまま継続できます。種目を分ける場合は別の種目として追加してください。", style = MaterialTheme.typography.bodySmall)
                        if (selectable) TextButton(onClick = { showChoice = true }, enabled = !state.busy) { Text("機種・種目を選択") }
                    }
                }
                if (skipped) {
                    Text("スキップ（未実施）", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { model.restore(exercise.id) }, enabled = !state.busy) { Text("スキップを解除") }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("前回の記録", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(current.previous?.let { "最終セット ${it.displayText()} · 計${current.previousSets.size}セット" } ?: "前回の記録はありません。", style = MaterialTheme.typography.bodyMedium)
                        if (current.previousSets.isNotEmpty()) TextButton(onClick = { showPrevious = true }) { Text("前回のセットを見る") }
                    }
                }
                if (exercise.notes.isNotBlank()) Text(exercise.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!bodyweight) QuantityStepper(weight, { weight = it }, true, !state.busy && !skipped, Modifier.weight(1f), profile?.loadType == LoadType.ASSISTANCE)
                    QuantityStepper(reps, { reps = it }, false, !state.busy && !skipped, Modifier.weight(1f))
                }
                if (stepImages != null && !bodyweight) Text("重量はダンベル1個分です。", style = MaterialTheme.typography.labelSmall)
                if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error)
                Text("目標：${exercise.targetReps}回 × ${exercise.targetSets}セット", style = MaterialTheme.typography.labelLarge)
                SetList(current.sets, exercise.targetSets, !state.busy, model::undo, onEdit = { model.clearError(); editingSetId = it })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { model.move(-1) }, enabled = !state.busy && active.session.currentPosition>0) { Text("前の種目") }
                    OutlinedButton(onClick = { model.move(1) }, enabled = !state.busy && active.session.currentPosition<active.exercises.lastIndex) { Text("次の種目") }
                }
                OutlinedButton(onClick = model::pause, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("中断して戻る") }
                TextButton(onClick = { confirmFinish = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("トレーニングを終了") }
                Spacer(Modifier.height(8.dp))
            }
            key(exercise.id, current.sets.size, state.busy) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(onClick = { keyboard?.hide();focus.clearFocus();model.completeSet(exercise.id,weight,reps,requestId) }, enabled = canSave, modifier = Modifier.fillMaxWidth().padding(vertical = if(tiny) 4.dp else 8.dp).heightIn(min = 56.dp)) {
                    Text(saveLabel, style = MaterialTheme.typography.titleMedium)
                }
            }
            }
        }
    }
    current.sets.firstOrNull { it.id == editingSetId }?.let { record ->
        SetEditDialog(record, current.sets.indexOf(record) + 1, state.busy, state.error, bodyweight = bodyweight,
            onDismiss = { model.clearError(); editingSetId = null },
            onSave = { weightValue, repsValue ->
                keyboard?.hide(); focus.clearFocus()
                model.updateSet(record.id, weightValue, repsValue) { editingSetId = null }
            })
    }
    if (showQueue) ExerciseQueueSheet(state,model,onDismiss = { showQueue = false })
    if (showChoice) AlertDialog(
        onDismissRequest = { showChoice = false }, title = { Text("使用する機種・種目") },
        text = { Column { options.forEach { choice -> TextButton(onClick = { showChoice = false;model.chooseVisual(exercise.id,choice.key) }, modifier = Modifier.fillMaxWidth()) { Text(choice.label) } } } },
        confirmButton = { TextButton(onClick = { showChoice = false }) { Text("閉じる") } },
    )
    if (showPrevious) AlertDialog(
        onDismissRequest = { showPrevious = false }, title = { Text("前回の全セット") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { current.previousSets.forEachIndexed { index,s -> Text("${index+1}. ${s.displayText()}") } } },
        confirmButton = { TextButton(onClick = { showPrevious = false }) { Text("閉じる") } },
    )
    if (confirmFinish) AlertDialog(
        onDismissRequest = { confirmFinish = false }, title = { Text("トレーニングを終了しますか？") },
        text = { Text("完了したセットを保存して終了します。セットが0件の場合は記録を残しません。") },
        confirmButton = { TextButton(onClick = { confirmFinish = false;model.finish() }) { Text("終了する") } },
        dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("続ける") } },
    )
}
