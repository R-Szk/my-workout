package com.myworkout.feature.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myworkout.data.SetRecord

@Composable
fun SetEditDialog(record: SetRecord, number: Int, busy: Boolean, error: String?, bodyweight: Boolean = false, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var weight by rememberSaveable(record.id) { mutableStateOf(record.weightKg?.toString() ?: "") }
    var reps by rememberSaveable(record.id) { mutableStateOf(record.reps.toString()) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("${number}セット目を編集") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!bodyweight) OutlinedTextField(weight, { weight = it }, label = { Text("重量（kg）") }, singleLine = true,
                    enabled = !busy, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(reps, { reps = it }, label = { Text("回数") }, singleLine = true,
                    enabled = !busy, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { keyboard?.hide(); focus.clearFocus(); onSave(weight, reps) }, enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp)) { Text(if (busy) "保存中…" else "変更を保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("キャンセル") } },
    )
}
