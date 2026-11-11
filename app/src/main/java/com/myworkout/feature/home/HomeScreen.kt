package com.myworkout.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.myworkout.data.*
import com.myworkout.feature.catalog.CatalogState
import com.myworkout.feature.workout.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(catalog: CatalogState, state: WorkoutUiState, model: WorkoutViewModel, retryCatalog: () -> Unit,
               history: List<WorkoutHistory>, historyError: Boolean, retryHistory: () -> Unit, onOpenGuide: () -> Unit) {
    var selected by rememberSaveable { mutableLongStateOf(-1L) }
    var legacy by rememberSaveable { mutableStateOf(false) }
    val menus = (catalog as? CatalogState.Ready)?.let { if (legacy && it.legacyRoutines.isNotEmpty()) it.legacyRoutines else it.routines }.orEmpty()
    val selectedId = menus.firstOrNull { it.routine.id == selected }?.routine?.id ?: menus.firstOrNull()?.routine?.id ?: -1L
    val active = state.active
    val sameRoutine = active?.session?.routineId == selectedId
    var pendingRoutine by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingSession by rememberSaveable { mutableStateOf<Long?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var deletingWorkout by rememberSaveable { mutableStateOf<Long?>(null) }
    val actual = history.filter { h -> h.exercises.any { it.exercise.status != ExerciseStatus.SKIPPED && it.sets.isNotEmpty() } }
    val deleteTarget=actual.firstOrNull { it.session.id==deletingWorkout }
    val today=LocalDate.now();val start=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val thisWeek=actual.count { val date=Instant.ofEpochMilli(it.session.startedAt).atZone(ZoneId.systemDefault()).toLocalDate();!date.isBefore(start) && date.isBefore(start.plusDays(7)) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(vertical=16.dp)) {
            item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text("MY WORKOUT",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                TextButton(onClick=onOpenGuide) { Text("動作を見る") }
                TextButton(onClick={showHistory=true}) { Text("履歴") }
            } }
            item { Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("今日のトレーニング",style=MaterialTheme.typography.headlineLarge)
                Text("${today.monthValue}月${today.dayOfMonth}日 · 自分のペースで続けよう",color=MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("今週の実施回数",style=MaterialTheme.typography.titleSmall)
                    Text("$thisWeek / 2 回",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(progress={ (thisWeek/2f).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
                Text("セットを記録して終了したトレーニングを集計",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            } } }
            if(state.active!=null) item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text("中断中：${if(state.active.session.routineKey?.startsWith("home.")==true) "自重トレーニング" else "マシントレーニング"}",style=MaterialTheme.typography.labelMedium)
                    Text(state.active.session.routineName,style=MaterialTheme.typography.titleLarge)
                    Text("${state.active.session.currentPosition+1} / ${state.active.exercises.size}種目 · ${state.active.exercises.sumOf { it.sets.size }}セット記録済み")
                    if(!sameRoutine) OutlinedButton(onClick=model::resume,enabled=!state.busy) { Text("中断中のメニューを再開") }
                }
            } }
            if(state.message!=null) item { Text(state.message,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary) }
            if(state.error!=null) item { Column { Text(state.error,color=MaterialTheme.colorScheme.error);TextButton(onClick=model::reload,enabled=!state.busy){Text("再読み込み")} } }
            if(historyError) item { TextButton(onClick=retryHistory) { Text("履歴を再読み込み") } }
            when(catalog) {
                CatalogState.Loading -> item { CircularProgressIndicator() }
                CatalogState.Error -> item { Column { Text("メニューを読み込めませんでした。");Button(onClick=retryCatalog){Text("再読み込み")} } }
                is CatalogState.Ready -> {
                    item {
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(catalog.legacyRoutines.isNotEmpty()) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected=!legacy,onClick={legacy=false;selected=-1L},enabled=!state.busy,label={Text("自重トレーニング")})
                                FilterChip(selected=legacy,onClick={legacy=true;selected=-1L},enabled=!state.busy,label={Text("マシントレーニング")})
                            }
                        }
                    }
                    item { Text("メニューを選択",style=MaterialTheme.typography.titleMedium) }
                    item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        menus.forEach { item ->
                            Card(Modifier.weight(1f).clickable(enabled=!state.busy){selected=item.routine.id},colors=CardDefaults.cardColors(containerColor=if(selectedId==item.routine.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(16.dp).heightIn(min=90.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Text(item.routine.name,style=MaterialTheme.typography.titleMedium)
                                    Text("${item.exercises.size}種目 · ${item.exercises.sumOf { it.exercise.targetSets }}セット予定",style=MaterialTheme.typography.labelSmall)
                                    if(selectedId==item.routine.id) Text("選択中",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    } }
                    item { Text("今日のメニュー",style=MaterialTheme.typography.titleMedium) }
                    val chosen=menus.first { it.routine.id==selectedId }
                    items(chosen.exercises,key={it.exercise.position}) { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            Text("${item.exercise.position+1}",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                Text("${item.exercise.position+1}. ${item.machine.name}",style=MaterialTheme.typography.titleMedium)
                                Text("${item.exercise.targetReps}回 × ${item.exercise.targetSets}セット",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.3f))
                    }
                    item { Text("最初の2週間は各2セットから始めましょう。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Button(onClick={
                when {
                    active==null -> model.start(selectedId)
                    sameRoutine -> model.resume()
                    else -> { pendingRoutine=selectedId;pendingSession=active.session.id }
                }
            },enabled=catalog is CatalogState.Ready && !state.loading && !state.busy && state.error==null,
                modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {
                Text(if(sameRoutine) "トレーニングを再開" else if(active!=null) "選択したメニューで開始" else "トレーニング開始",style=MaterialTheme.typography.titleMedium)
            }
        }
    }
    if(pendingRoutine!=null && pendingSession!=null) AlertDialog(
        onDismissRequest={pendingRoutine=null;pendingSession=null},
        title={Text("中断中の記録を保存して切り替えますか？")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("中断中：${active?.session?.routineName ?: ""}")
            val target=(catalog as? CatalogState.Ready)?.let { it.routines+it.legacyRoutines }?.firstOrNull { it.routine.id==pendingRoutine }
            Text("開始するメニュー：${target?.routine?.name ?: ""}")
            Text("記録済みのセットを保存して終了し、選択したメニューを新しく開始します。")
        }},
        confirmButton={TextButton(onClick={
            val routine=pendingRoutine;val session=pendingSession
            pendingRoutine=null;pendingSession=null
            if(routine!=null && session!=null) model.switchRoutine(session,routine)
        },enabled=!state.busy){Text("保存して開始")}},
        dismissButton={TextButton(onClick={pendingRoutine=null;pendingSession=null}){Text("キャンセル")}},
    )
    if(showHistory && deleteTarget==null) AlertDialog(onDismissRequest={showHistory=false},title={Text("トレーニング履歴")},text={
        if(actual.isEmpty()) Text("まだ記録がありません。")
        else LazyColumn(Modifier.heightIn(max=420.dp)) { items(actual,key={it.session.id}) { item ->
            val date=Instant.ofEpochMilli(item.session.startedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M/d HH:mm"))
            Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick={showHistory=false;model.showSummary(item.session.id)},enabled=!state.busy,modifier=Modifier.weight(1f)) {
                    Text("$date · ${item.session.routineName}\n${item.exercises.sumOf { it.sets.size }}セット",Modifier.fillMaxWidth())
                }
                TextButton(onClick={model.clearError();deletingWorkout=item.session.id},enabled=!state.busy,
                    modifier=Modifier.heightIn(min=48.dp).semantics { contentDescription="ワークアウト${item.session.id}を削除" }) {
                    Text("削除",color=MaterialTheme.colorScheme.error)
                }
            }
        } }
    },confirmButton={TextButton(onClick={showHistory=false}){Text("閉じる")}})
    if(deletingWorkout!=null && deleteTarget!=null) AlertDialog(
        onDismissRequest={if(!state.busy) {deletingWorkout=null;model.clearError()}},
        title={Text("ワークアウトを削除しますか？")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            val date=Instant.ofEpochMilli(deleteTarget.session.startedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy/M/d HH:mm"))
            Text("$date · ${deleteTarget.session.routineName}")
            Text("${deleteTarget.exercises.sumOf { it.sets.size }}セットの記録を削除します。この操作は取り消せません。")
            if(state.error!=null) Text(state.error,color=MaterialTheme.colorScheme.error)
        }},
        confirmButton={TextButton(onClick={model.deleteWorkoutHistory(deleteTarget.session.id) {deletingWorkout=null}},enabled=!state.busy) {
            Text(if(state.busy) "削除中…" else "削除する",color=MaterialTheme.colorScheme.error)
        }},
        dismissButton={TextButton(onClick={deletingWorkout=null;model.clearError()},enabled=!state.busy){Text("キャンセル")}},
    )
}
