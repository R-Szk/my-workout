package com.myworkout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.myworkout.domain.ExerciseProgress
import com.myworkout.data.LoadType

@Composable
fun ProgressChart(progress: ExerciseProgress) {
    val bodyweight = progress.loadType == LoadType.BODYWEIGHT
    var repetitions by remember(progress.key) { mutableStateOf(bodyweight) }
    val points = if (repetitions) progress.repetitionPoints else progress.points
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = .3f)
    val unit = if (repetitions) "回" else "kg"
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if(!bodyweight) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!repetitions, { repetitions=false }, { Text(if(progress.loadType==LoadType.ASSISTANCE) "補助重量" else "重量") })
            FilterChip(repetitions, { repetitions=true }, { Text("同重量の回数") })
        }
        Text(if(bodyweight) "各日の最多回数" else if(repetitions) "${progress.currentWeightKg} kgでの最多回数" else "各日の代表セット · $unit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (points.isEmpty()) Text("グラフに表示できる記録がありません。")
        else {
            val values = points.map { if(repetitions) it.reps.toDouble() else requireNotNull(it.weightKg) }
            val ceiling = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.height(140.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("${if(repetitions) ceiling.toInt().toString() else ceiling.toString()} $unit", style = MaterialTheme.typography.labelSmall)
                    Text("0", style = MaterialTheme.typography.labelSmall)
                }
                Canvas(Modifier.weight(1f).height(140.dp).semantics { contentDescription = "成長グラフ。${points.size}日分。" + points.joinToString { "${it.date} ${it.weightKg?.let { w -> "${w}kg " } ?: ""}${it.reps}回" } }) {
                    val pad=8.dp.toPx();val width=(size.width-2*pad).coerceAtLeast(1f);val height=(size.height-2*pad).coerceAtLeast(1f)
                    for (line in 0..2) drawLine(grid,Offset(pad,pad+height*line/2),Offset(pad+width,pad+height*line/2),1.dp.toPx())
                    val coords=values.mapIndexed { index,value -> Offset(if(values.size==1) pad+width/2 else pad+width*index/(values.size-1),pad+height*(1-value/ceiling).toFloat()) }
                    if(coords.size>1) { val path=Path();coords.forEachIndexed { i,p -> if(i==0) path.moveTo(p.x,p.y) else path.lineTo(p.x,p.y) };drawPath(path,color,style=Stroke(2.5.dp.toPx(),cap=StrokeCap.Round)) }
                    coords.forEach { drawCircle(color,4.dp.toPx(),it) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(points.first().date.toString(),style=MaterialTheme.typography.labelSmall)
                if(points.size>1) Text(points.last().date.toString(),style=MaterialTheme.typography.labelSmall)
            }
            if(points.size==1) Text("記録は1日分です。次の記録から推移を表示します。",style=MaterialTheme.typography.bodySmall)
        }
    }
}
