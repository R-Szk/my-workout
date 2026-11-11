package com.myworkout.data

import org.junit.Assert.*
import org.junit.Test

class ExerciseStepsTest {
    @Test fun sixExercisesUseManifestOrderAndThreeDistinctSteps() {
        assertEquals(listOf("push_up", "lateral_raise", "triceps_extension", "one_arm_row", "dumbbell_curl", "squat"), ExerciseSteps.all.map { it.id })
        assertEquals(listOf("A","A","A","B","B","B"), ExerciseSteps.all.map { it.menu })
        ExerciseSteps.all.forEach { exercise ->
            assertEquals(3, exercise.images.size)
            assertEquals(3, exercise.images.toSet().size)
            assertEquals(exercise, ExerciseSteps.find("home.${exercise.id}"))
        }
    }
    @Test fun gymProfilesAndUnknownKeysNeverResolveToHomeStepImages() {
        assertNull(ExerciseSteps.find("lateral_raise"))
        assertNull(ExerciseSteps.find("preacher_curl"))
        assertNull(ExerciseSteps.find(null))
        assertNull(ExerciseSteps.find("home.unknown"))
    }
}
