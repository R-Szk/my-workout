package com.myworkout.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.myworkout.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkoutVisualHistoryTest {
    private lateinit var db: AppDatabase
    private lateinit var controller: SessionController
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        WorkoutRepository(db).seedIfEmpty(); controller = SessionController(db)
    }
    @After fun close() { db.close() }
    @Test fun previousWholeSessionHasThreeSetsButSuggestionCreatesNoRecords() = runBlocking {
        val first = controller.start(1)
        val id = controller.resumeActive()!!.exercises.first().exercise.id
        repeat(3) { controller.completeSet(id, 20.0, 10) }
        controller.finish(first); controller.start(1)
        val next = controller.resumeActive()!!.exercises.first()
        assertEquals(3, next.previousSets.size)
        assertEquals(20.0, next.previous!!.weightKg!!, 0.0)
        assertTrue(next.sets.isEmpty())
    }
    @Test fun legacyUnclassifiedActiveRecordsCanContinueWithoutRewritingTheirKind() = runBlocking {
        controller.start(1)
        val exercise = controller.resumeActive()!!.exercises[4].exercise
        db.workoutSessionDao().insertRecord(SetRecord(sessionExerciseId=exercise.id,sequence=1,weightKg=30.0,reps=9,completedAt=1,requestId="legacy"))
        controller.completeSet(exercise.id,30.0,10)
        val continued = controller.resumeActive()!!.exercises[4]
        assertEquals(2,continued.sets.size)
        assertNull(continued.exercise.visualKey)
    }

    @Test fun selectionIsSnapshottedAndRecordedVariantCannotBeReclassified() = runBlocking {
        val first = controller.start(1)
        val id = controller.resumeActive()!!.exercises[4].exercise.id
        try { controller.completeSet(id, 30.0, 10); fail("Select a variant first") } catch (_: IllegalArgumentException) { }
        controller.chooseVisual(id, "assisted_dip")
        controller.completeSet(id, 30.0, 10)
        try { controller.chooseVisual(id, "assisted_chin"); fail("Cannot reclassify records") } catch (_: IllegalArgumentException) { }
        controller.finish(first)
        controller.start(1)
        val later = controller.resumeActive()!!.exercises[4]
        assertEquals("assisted_dip", later.exercise.visualKey)
        controller.chooseVisual(later.exercise.id, "assisted_chin")
        assertTrue(controller.resumeActive()!!.exercises[4].previousSets.isEmpty())
        assertEquals("assisted_dip", db.workoutSessionDao().exercise(id)!!.visualKey)
    }
}
