package com.myworkout.feature.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.myworkout.data.*
import com.myworkout.ui.components.*

@Composable
fun ExerciseGuideScreen(onRecord: ((ExerciseStepSet) -> Unit)? = null, hasActive: Boolean = false, onClose: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val exercise = ExerciseSteps.all[selected.coerceIn(ExerciseSteps.all.indices)]
    val mainColor = Color(0xFF2DD4BF)
    BackHandler(onBack = onClose)
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxHeight < 420.dp
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(if (compact) exercise.name else "トレーニングの動き", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onClose) { Text("戻る") }
        }
        if (!compact) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseSteps.all.forEachIndexed { index, item ->
                FilterChip(selected == index, { selected = index }, { Text("${item.menu} · ${item.name}") })
            }
        }
        Text(exercise.name, style = MaterialTheme.typography.titleLarge)
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExerciseStepImages(exercise, Modifier.weight(2.5f).fillMaxHeight())
            Card(Modifier.weight(1f).fillMaxHeight()) {
                BodyMap(exercise.muscles.associateWith { MuscleRole.PRIMARY }, Modifier.fillMaxSize().padding(6.dp), primaryColor = mainColor)
            }
        }
        if (!compact) Text("1 → 2 → 3 → 2 → 1の順に切り替わります。表示時間は変更できます。", style = MaterialTheme.typography.bodySmall)
        if(onRecord != null) Button(onClick={onRecord(exercise)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text(if(hasActive) "トレーニングを再開" else "この種目から記録を始める") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { selected-- }, enabled = selected > 0) { Text("前の種目") }
            OutlinedButton(onClick = { selected++ }, enabled = selected < ExerciseSteps.all.lastIndex) { Text("次の種目") }
        }
    }
    }
}
