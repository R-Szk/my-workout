package com.myworkout.feature.animation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myworkout.data.*
import com.myworkout.ui.animation.ExerciseAnimation
import com.myworkout.ui.components.BodyMap

@Composable
fun CurlPrototypeScreen(onClose: () -> Unit) {
    var paused by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    var muscles by rememberSaveable { mutableStateOf(false) }
    val turquoise = Color(0xFF2DD4BF)
    val targets = remember { mapOf(Muscle.BICEPS to MuscleRole.PRIMARY) }
    BackHandler { onClose() }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val compact = maxHeight < 420.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (!compact) Text("自宅トレーニング · 動作の試作", style = MaterialTheme.typography.labelMedium, color = turquoise)
                    Text("ダンベルカール", style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("閉じる") }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(Modifier.weight(2.5f).fillMaxHeight()) {
                    ExerciseAnimation(paused, { paused = !paused }, Modifier.fillMaxSize().padding(4.dp))
                }
                Card(Modifier.weight(1f).fillMaxHeight().clickable { muscles = true }.semantics { contentDescription = "鍛えるところの拡大" }) {
                    Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        if (!compact) Text("鍛えるところ", style = MaterialTheme.typography.labelSmall)
                        BodyMap(targets, Modifier.weight(1f).fillMaxWidth(), primaryColor = turquoise)
                        Text("力こぶ", style = MaterialTheme.typography.labelMedium, color = turquoise)
                        Text("正面／背面", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text("肘は体の横で動かさず、ゆっくり上げ下げ。", style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1往復 約2.5秒", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { details = true }) { Text("フォームのコツ") }
            }
            Button(onClick = { paused = !paused }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (paused) "再生する" else "一時停止")
            }
        }
    }
    if (details) AlertDialog(onDismissRequest = { details = false }, title = { Text("フォームのコツ") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("手のひらを前に向け、肘を体の横に保ちます。肩や体を振らずに持ち上げ、ゆっくり戻しましょう。\n\nイラストをタップしても停止・再開できます。動作と見た目を確認する試作のため、この画面では記録しません。")
        } }, confirmButton = { TextButton(onClick = { details = false }) { Text("閉じる") } })
    if (muscles) AlertDialog(onDismissRequest = { muscles = false }, title = { Text("力こぶを使う運動") },
        text = { Column { BodyMap(targets, Modifier.fillMaxWidth().height(280.dp), primaryColor = turquoise); Text("色のついたところ：上腕二頭筋\nグレー：その他の部位") } },
        confirmButton = { TextButton(onClick = { muscles = false }) { Text("閉じる") } })
}
