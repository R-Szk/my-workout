package com.myworkout.feature.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myworkout.data.Machine

@Composable
fun ExercisePicker(machines: List<Machine>, onSelect: (Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("追加する種目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("末尾に追加します。目標は同じメニューの設定、未登録なら2セット・10回です。")
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(machines, key = { it.id }) { machine ->
                        TextButton(onClick = { onSelect(machine.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(machine.name, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
