package com.myworkout.data

import com.myworkout.R

/** Static, original PNGs from design-assets/manifest.json. No frame animation or geometry changes. */
data class ExerciseStepSet(val id: String, val name: String, val menu: String, val images: List<Int>, val muscles: Set<Muscle>) {
    val visualKey: String get() = "home.$id"
}
object ExerciseSteps {
    val all = listOf(
        ExerciseStepSet("push_up", "プッシュアップ", "A", listOf(R.drawable.workout_push_up_step_1, R.drawable.workout_push_up_step_2, R.drawable.workout_push_up_step_3), setOf(Muscle.CHEST)),
        ExerciseStepSet("lateral_raise", "ダンベル・サイドレイズ", "A", listOf(R.drawable.workout_lateral_raise_step_1, R.drawable.workout_lateral_raise_step_2, R.drawable.workout_lateral_raise_step_3), setOf(Muscle.SIDE_DELTS)),
        ExerciseStepSet("triceps_extension", "ダンベル・トライセプスエクステンション", "A", listOf(R.drawable.workout_triceps_extension_step_1, R.drawable.workout_triceps_extension_step_2, R.drawable.workout_triceps_extension_step_3), setOf(Muscle.TRICEPS)),
        ExerciseStepSet("one_arm_row", "ワンハンド・ダンベルロー", "B", listOf(R.drawable.workout_one_arm_row_step_1, R.drawable.workout_one_arm_row_step_2, R.drawable.workout_one_arm_row_step_3), setOf(Muscle.LATS)),
        ExerciseStepSet("dumbbell_curl", "ダンベルカール", "B", listOf(R.drawable.workout_dumbbell_curl_step_1, R.drawable.workout_dumbbell_curl_step_2, R.drawable.workout_dumbbell_curl_step_3), setOf(Muscle.BICEPS)),
        ExerciseStepSet("squat", "スクワット", "B", listOf(R.drawable.workout_squat_step_1, R.drawable.workout_squat_step_2, R.drawable.workout_squat_step_3), setOf(Muscle.QUADS, Muscle.GLUTES)),
    )
    // Namespace prevents a gym lateral-raise profile from displaying a dumbbell exercise.
    fun find(visualKey: String?): ExerciseStepSet? = all.firstOrNull { it.visualKey == visualKey }
}
