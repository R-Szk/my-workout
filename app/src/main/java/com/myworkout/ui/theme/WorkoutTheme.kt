package com.myworkout.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Emerald = Color(0xFF34D399)
val Navy = Color(0xFF090F1D)
val MainMuscleColor = Color(0xFFED514B)
val SecondaryMuscleColor = Color(0xFFF4A344)
private val WorkoutColors = darkColorScheme(
    primary = Emerald, onPrimary = Color(0xFF06271C), primaryContainer = Color(0xFF123E32),
    onPrimaryContainer = Color(0xFFB8F5DC),
    secondary = Emerald, onSecondary = Color(0xFF06271C),
    secondaryContainer = Color(0xFF123E32), onSecondaryContainer = Color(0xFFB8F5DC),
    surfaceContainerLowest = Color(0xFF0B1422), surfaceContainerLow = Color(0xFF111C2D),
    surfaceContainer = Color(0xFF152237), surfaceContainerHigh = Color(0xFF1A2B42),
    surfaceContainerHighest = Color(0xFF20324B),
    background = Navy, onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF121D2F), onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1A283D), onSurfaceVariant = Color(0xFFAFBED3),
    outline = Color(0xFF3C4E67), error = Color(0xFFFF8C94),
)
@Composable
fun WorkoutTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WorkoutColors, shapes = Shapes(
        small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp),
    ), content = content)
}
