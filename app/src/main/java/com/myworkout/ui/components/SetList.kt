package com.myworkout.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myworkout.data.SetRecord

@Composable
fun SetList(sets: List<SetRecord>, targetSets: Int, enabled: Boolean, onUndo: (Long) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("記録済み：${sets.size}セット", style = MaterialTheme.typography.titleMedium)
            repeat(maxOf(sets.size, targetSets.coerceIn(0, 100))) { index ->
                val record = sets.getOrNull(index)
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(if (record == null) "○" else "✓", color = if (record == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("${index+1}セット目", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(record?.let { "${it.weightKg} kg × ${it.reps}回" } ?: "未完了", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (record != null) TextButton(onClick = { onUndo(record.id) }, enabled = enabled) { Text("取り消し") }
                }
                if (index < maxOf(sets.size, targetSets.coerceIn(0,100))-1) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .3f))
            }
        }
    }
}
