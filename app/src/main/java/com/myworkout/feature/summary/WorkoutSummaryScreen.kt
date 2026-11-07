package com.myworkout.feature.summary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.myworkout.domain.WorkoutSummary
import com.myworkout.data.LoadType
import com.myworkout.ui.components.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutSummaryScreen(summary: WorkoutSummary, onHome: () -> Unit) {
    BackHandler(onBack=onHome)
    val started=Instant.ofEpochMilli(summary.history.session.startedAt).atZone(ZoneId.systemDefault())
    var selected by remember(summary.history.session.id) { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(vertical=20.dp)) {
            item { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("WORKOUT COMPLETE",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
                Text("トレーニング完了",style=MaterialTheme.typography.headlineLarge)
                Text("${summary.history.session.routineName} · ${started.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))}",color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("トレーニングを保存して終了しました。",style=MaterialTheme.typography.bodySmall)
            } }
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Metric("実施種目","${summary.exerciseCount}種目","実施種目数：${summary.exerciseCount}",Modifier.weight(1f))
                Metric("完了セット","${summary.setCount}セット","完了セット数：${summary.setCount}",Modifier.weight(1f))
                val seconds=summary.durationMillis/1000
                Metric("経過時間",if(seconds<60) "${seconds}秒" else "${seconds/60}分${seconds%60}秒","経過時間：${seconds}秒",Modifier.weight(1f))
            } }
            item { Text("時間は開始から終了まで（中断を含む）。",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            item { Text("記録の変化",style=MaterialTheme.typography.titleLarge) }
            if(summary.progress.isEmpty()) item { Text(if(summary.setCount==0) "完了したセットはありません。" else "種目未分類のため比較できません。") }
            else item { Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    if(summary.progress.size>1) {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            items(summary.progress.size) { index -> FilterChip(index==selected,{selected=index},{Text(summary.progress[index].name)}) }
                        }
                    }
                    val progress=summary.progress[selected.coerceIn(summary.progress.indices)]
                    Text(progress.name,style=MaterialTheme.typography.titleMedium)
                    Text("今回 ${if(progress.loadType==LoadType.ASSISTANCE) "補助 " else ""}${progress.currentWeightKg} kg × ${progress.currentReps}回",style=MaterialTheme.typography.titleLarge)
                    if(progress.weightChange==null) Text("前回の比較記録はありません。",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        val change=progress.weightChange
                        Text("${if(progress.loadType==LoadType.ASSISTANCE) "補助重量" else "重量"}：${if(change>0) "+" else ""}$change kg",color=MaterialTheme.colorScheme.primary)
                        progress.repetitionChange?.let { Text("同重量の回数：${if(it>0) "+" else ""}$it 回") }
                            ?: Text("重量が異なるため回数の増減は比較しません。",style=MaterialTheme.typography.bodySmall)
                    }
                    if(progress.loadType==LoadType.ASSISTANCE) Text("補助重量は少ないほど補助が弱くなります。",style=MaterialTheme.typography.bodySmall)
                    ProgressChart(progress)
                }
            } }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("今日鍛えた部位",style=MaterialTheme.typography.titleMedium)
                BodyMap(summary.muscles,Modifier.fillMaxWidth().height(240.dp))
                MuscleLegend(summary.muscles)
                if(summary.unknownExercises>0) Text("${summary.unknownExercises}種目の部位は未確定です。")
                Text("色は実施種目の対象部位です。筋肥大や筋活動量の測定ではありません。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            } } }
            item { Card(Modifier.fillMaxWidth()) { Box(Modifier.padding(16.dp)) { MonthlyActivity(YearMonth.from(started),summary.trainingDays,summary.trainingCount) } } }
        }
        Button(onClick=onHome,modifier=Modifier.fillMaxWidth().padding(16.dp).heightIn(min=56.dp)) { Text("ホームへ") }
    }
}

@Composable
private fun Metric(label:String,value:String,description:String,modifier:Modifier) {
    Card(modifier.semantics { contentDescription=description }) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value,style=MaterialTheme.typography.titleLarge)
        }
    }
}
