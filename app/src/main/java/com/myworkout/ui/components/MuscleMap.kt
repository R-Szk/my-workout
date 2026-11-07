package com.myworkout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.myworkout.data.*
import com.myworkout.ui.theme.*

// The unadorned body outlines follow the supplied muscle SVGs. Colored regions are
// explanatory regions, not measurements. Profile roles are independent of machine photos.
private val bodyPaths = listOf(
    "M150 132 Q190 116 230 132 L244 186 227 261 217 299 163 299 153 261 136 186 Z",
    "M150 143 Q133 135 121 153 L105 223 114 282 129 282 125 222 151 183 Z M230 143 Q247 135 259 153 L275 223 266 282 251 282 255 222 229 183 Z",
    "M165 296 L152 382 153 458 174 458 181 385 190 334 199 385 206 458 227 458 228 382 215 296 Z",
    "M153 458 L145 482 Q146 488 175 486 L174 458 Z M206 458 L205 486 Q234 488 235 482 L227 458 Z",
).map { PathParser.createPathFromPathData(it)!!.asComposePath() }

@Composable
fun BodyMap(muscles: Map<Muscle, MuscleRole>, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "筋肉図。正面と背面。" + muscles.entries.joinToString { "${it.key.label}:${if (it.value == MuscleRole.PRIMARY) "メイン" else "補助"}" } }) {
        val scale = minOf(size.width / 390f, size.height / 460f)
        val x = (size.width - 390f * scale) / 2
        val y = (size.height - 460f * scale) / 2
        withTransform({ translate(x, y); scale(scale, scale, Offset.Zero) }) {
            for (back in listOf(false, true)) withTransform({ translate(if (back) 205f else 0f, -35f); translate(-95f, 0f) }) {
                val base = Color(0xFF62728A)
                drawOval(base, Offset(164f, 48f), Size(52f, 66f))
                drawRoundRect(base, Offset(173f, 109f), Size(34f, 30f), CornerRadius(10f))
                bodyPaths.forEach { drawPath(it, base); drawPath(it, Color(0xFF9CACBF), style = Stroke(1.5f)) }
                fun area(muscle: Muscle, cx: Float, cy: Float, rx: Float, ry: Float) {
                    val role = muscles[muscle] ?: return
                    drawOval(if (role == MuscleRole.PRIMARY) MainMuscleColor else SecondaryMuscleColor, Offset(cx-rx,cy-ry), Size(rx*2,ry*2))
                }
                fun paired(m: Muscle, left: Float, right: Float, cy: Float, rx: Float, ry: Float) { area(m,left,cy,rx,ry);area(m,right,cy,rx,ry) }
                if (!back) {
                    area(Muscle.CHEST,190f,154f,39f,23f); area(Muscle.UPPER_CHEST,190f,140f,38f,12f)
                    paired(Muscle.FRONT_DELTS,141f,239f,151f,13f,22f)
                    paired(Muscle.SIDE_DELTS,129f,251f,157f,10f,21f)
                    paired(Muscle.BICEPS,126f,254f,202f,9f,23f)
                    area(Muscle.ABS,190f,231f,20f,49f); paired(Muscle.OBLIQUES,159f,221f,233f,10f,39f)
                    paired(Muscle.QUADS,171f,209f,365f,14f,56f)
                    paired(Muscle.ADDUCTORS,183f,197f,336f,7f,29f)
                } else {
                    paired(Muscle.LATS,166f,214f,217f,17f,43f); area(Muscle.MID_BACK,190f,163f,28f,27f)
                    paired(Muscle.REAR_DELTS,141f,239f,151f,14f,20f)
                    paired(Muscle.TRICEPS,126f,254f,204f,10f,27f)
                    paired(Muscle.ERECTORS,180f,200f,245f,8f,37f)
                    paired(Muscle.GLUTES,174f,206f,313f,18f,23f)
                    paired(Muscle.GLUTE_MED,162f,218f,296f,12f,14f)
                    paired(Muscle.HAMSTRINGS,171f,209f,365f,12f,45f)
                }
            }
        }
    }
}

@Composable
fun MuscleMap(profile: VisualProfile?, modifier: Modifier = Modifier) {
    val muscles = MuscleCatalog.combine(listOfNotNull(profile))
    var expanded by remember { mutableStateOf(false) }
    Card(modifier.clickable { expanded = true }.semantics { contentDescription = "筋肉図" }) {
        LocalArtwork(profile?.muscleImage, "${profile?.label ?: "種目未設定"}の筋肉図", Modifier.fillMaxSize()) {
            BodyMap(muscles, Modifier.fillMaxSize().padding(4.dp))
        }
    }
    if (expanded) AlertDialog(
        onDismissRequest = { expanded = false }, title = { Text(profile?.label ?: "種目未設定") },
        text = { Column {
            LocalArtwork(profile?.muscleImage, "筋肉図", Modifier.fillMaxWidth().height(300.dp)) { BodyMap(muscles, Modifier.fillMaxWidth().height(300.dp)) }
            MuscleLegend(muscles)
        } }, confirmButton = { TextButton(onClick = { expanded = false }) { Text("閉じる") } },
    )
}

@Composable
fun MuscleLegend(muscles: Map<Muscle, MuscleRole>) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("● メイン：" + muscles.filterValues { it == MuscleRole.PRIMARY }.keys.joinToString("・") { it.label }.ifEmpty { "未設定" }, color = MainMuscleColor, style = MaterialTheme.typography.bodySmall)
        Text("● 補助：" + muscles.filterValues { it == MuscleRole.SECONDARY }.keys.joinToString("・") { it.label }.ifEmpty { "なし／未設定" }, color = SecondaryMuscleColor, style = MaterialTheme.typography.bodySmall)
        Text("グレー：その他の部位。筋肉図は説明用です。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
