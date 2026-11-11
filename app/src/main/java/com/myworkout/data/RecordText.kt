package com.myworkout.data

fun SetRecord.displayText(): String = weightKg?.let { "$it kg × ${reps}回" } ?: "${reps}回"
