package com.myworkout.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myworkout.domain.InputAdjustment

@Composable
fun QuantityStepper(value: String, onChange: (String) -> Unit, weight: Boolean, enabled: Boolean, modifier: Modifier = Modifier, assistance: Boolean = false) {
    val name = if (weight) "重量" else "回数"
    Card(modifier) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (weight) (if (assistance) "補助重量 · kg" else "重量 · kg") else "回数 · 回", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value, onChange, Modifier.fillMaxWidth(), singleLine = true, enabled = enabled,
                textStyle = MaterialTheme.typography.headlineMedium,
                keyboardOptions = KeyboardOptions(keyboardType = if (weight) KeyboardType.Decimal else KeyboardType.Number),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (direction in listOf(-1, 1)) OutlinedButton(
                    onClick = { onChange(if (weight) InputAdjustment.weight(value,direction) else InputAdjustment.reps(value,direction)) },
                    enabled = enabled,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = name + if (direction == 1) "を増やす" else "を減らす" },
                ) { Text(if (direction == 1) "＋" else "−", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}
