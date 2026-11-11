package com.myworkout.ui.animation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun ExerciseAnimation(paused: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val assets = LocalContext.current.assets
    val artwork by produceState<Result<CurlArtwork>?>(null, assets) {
        value = withContext(Dispatchers.IO) { runCatching { CurlArtwork.load(assets) } }
    }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val playing = !paused && lifecycle.isAtLeast(Lifecycle.State.RESUMED) && artwork?.isSuccess == true
    val progress = rememberSaveable { mutableDoubleStateOf(0.0) }
    LaunchedEffect(playing) {
        if (playing) {
            var previous: Long? = null
            while (isActive) withFrameNanos { now ->
                previous?.let { progress.doubleValue = CurlPose.advance(progress.doubleValue, now - it, true) }
                previous = now
            }
        }
    }
    val art = artwork?.getOrNull()
    if (art == null) Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(if (artwork?.isFailure == true) "試作素材を読み込めませんでした。" else "準備中…")
    } else Canvas(modifier.clickable(role = Role.Button, onClickLabel = if (paused) "再生" else "一時停止", onClick = onToggle)
        .semantics { contentDescription = "カール動作アニメーション"; stateDescription = if (playing) "再生中" else "停止中" }) {
        drawCurlCharacter(art, CurlPose.at(progress.doubleValue.toFloat()))
    }
}
