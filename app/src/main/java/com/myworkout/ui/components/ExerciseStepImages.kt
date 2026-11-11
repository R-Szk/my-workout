package com.myworkout.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.myworkout.data.ExerciseStepSet
import com.myworkout.domain.StepPlayback
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ExerciseStepImages(exercise: ExerciseStepSet, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences("exercise_image_playback", Context.MODE_PRIVATE) }
    var interval by remember { mutableLongStateOf(preferences.getLong("interval_ms", StepPlayback.DEFAULT_INTERVAL_MILLIS).coerceIn(100L,60000L)) }
    var tick by rememberSaveable(exercise.id) { mutableLongStateOf(0L) }
    var expanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    DisposableEffect(preferences) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "interval_ms") interval = prefs.getLong(key, StepPlayback.DEFAULT_INTERVAL_MILLIS).coerceIn(100L,60000L)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val playing = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && !settings
    LaunchedEffect(exercise.id, interval, playing) {
        if (playing) while (isActive) {
            delay(interval)
            tick = (tick + 1) % 4
        }
    }
    val frame = StepPlayback.frameAt(tick)
    Card(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val showControls = maxHeight >= 100.dp
            Column(Modifier.fillMaxSize().padding(4.dp)) {
                StepFrame(exercise, frame, Modifier.weight(1f).fillMaxWidth().clickable { expanded = true })
                if (showControls) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("${frame+1} / 3", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { settings = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("表示時間：${StepPlayback.secondsText(interval)}秒", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
    if (expanded) AlertDialog(onDismissRequest = { expanded = false }, title = { Text(exercise.name) },
        text = { StepFrame(exercise, frame, Modifier.fillMaxWidth().height(300.dp)) },
        dismissButton = { TextButton(onClick = { settings = true }) { Text("表示時間：${StepPlayback.secondsText(interval)}秒") } },
        confirmButton = { TextButton(onClick = { expanded = false }) { Text("閉じる") } })
    if (settings) StepIntervalDialog(interval, onDismiss = { settings = false }, onSave = { value ->
        interval = value
        preferences.edit().putLong("interval_ms", value).apply()
        settings = false
    })
}

@Composable
private fun StepFrame(exercise: ExerciseStepSet, frame: Int, modifier: Modifier) {
    // Fit the entire shared 900x900 canvas. Never trim to the person or move/scale per frame.
    Image(painterResource(exercise.images[frame]), "${exercise.name} ステップ${frame+1}", modifier, alignment = androidx.compose.ui.Alignment.Center, contentScale = ContentScale.Fit)
}

@Composable
private fun StepIntervalDialog(interval: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var seconds by rememberSaveable { mutableStateOf(StepPlayback.secondsText(interval)) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("1枚の表示時間") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(seconds, { seconds = it; error = false }, label = { Text("秒") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = error,
                supportingText = { Text(if (error) "0.1〜60秒で入力してください。" else "0.1〜60秒。小数も入力できます。") })
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (value in listOf("0.5", "1", "1.5", "2", "3")) FilterChip(seconds == value, { seconds = value; error = false }, { Text("${value}秒") })
            }
        }
    }, confirmButton = { TextButton(onClick = { val value = StepPlayback.parseIntervalMillis(seconds); if (value == null) error = true else onSave(value) }) { Text("適用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } })
}
