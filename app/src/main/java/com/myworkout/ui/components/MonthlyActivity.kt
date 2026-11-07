package com.myworkout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.*

@Composable
fun MonthlyActivity(month: YearMonth, days: Set<LocalDate>, count: Int) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("${month.year}年${month.monthValue}月 · ${count}回",style=MaterialTheme.typography.titleMedium)
        Row { listOf("日","月","火","水","木","金","土").forEach { Text(it,Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
        val offset=month.atDay(1).dayOfWeek.value%7
        for(week in 0 until (offset+month.lengthOfMonth()+6)/7) Row {
            for(dayOfWeek in 0..6) {
                val number=week*7+dayOfWeek-offset+1
                Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp),contentAlignment=Alignment.Center) {
                    if(number in 1..month.lengthOfMonth()) {
                        val date=month.atDay(number);val active=date in days
                        Box(Modifier.fillMaxSize().background(if(active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .semantics { contentDescription="$date ${if(active) "トレーニング実施" else "記録なし"}" },contentAlignment=Alignment.Center) {
                            Text(number.toString(),color=if(active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Text("実績のある終了済みトレーニングを表示",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
