package com.myworkout.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun MachinePhoto(path: String?, machineName: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier.clickable { expanded = true }.semantics { contentDescription = "マシン画像" }) {
        LocalArtwork(path, machineName, Modifier.fillMaxSize()) {
            Column(Modifier.padding(12.dp)) {
                Text("マシン", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(machineName, style = MaterialTheme.typography.bodySmall)
                Text(if (path == null) "機種・種目を選択" else "画像未配置", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (expanded) AlertDialog(
        onDismissRequest = { expanded = false }, title = { Text(machineName) },
        text = { LocalArtwork(path, machineName, Modifier.fillMaxWidth().height(350.dp)) { Text("画像がありません。名前で確認して記録を続けられます。") } },
        confirmButton = { TextButton(onClick = { expanded = false }) { Text("閉じる") } },
    )
}
