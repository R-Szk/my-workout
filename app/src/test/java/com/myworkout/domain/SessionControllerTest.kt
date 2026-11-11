package com.myworkout.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.myworkout.data.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionControllerTest {
    private lateinit var db: AppDatabase
    private lateinit var controller: SessionController

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        WorkoutRepository(db).seedIfEmpty()
        controller = SessionController(db)
    }
    @After fun close() { db.close() }

    @Test fun startSnapshotsMenuAndReturnsExistingUnfinishedSession() = runBlocking {
        val id = controller.start(1)
        assertEquals(id, controller.start(2))
        val workout = controller.resumeActive()!!
        assertEquals(id, workout.session.id)
        assertEquals(5, workout.exercises.size)
        assertEquals("チェストプレス", workout.exercises.first().exercise.machineName)
        db.openHelper.writableDatabase.execSQL("UPDATE machines SET name = '変更後' WHERE id = 1")
        db.openHelper.writableDatabase.execSQL("UPDATE routine_exercises SET targetSets = 9 WHERE routineId = 1")
        val snapshot = controller.resumeActive()!!.exercises.first().exercise
        assertEquals("チェストプレス", snapshot.machineName)
        assertEquals(3, snapshot.targetSets)
    }

    @Test fun recordsEachIntentOnceButAllowsIdenticalConsecutiveSets() = runBlocking {
        controller.start(1)
        val exercise = controller.resumeActive()!!.exercises.first().exercise.id
        val first = controller.completeSet(exercise, 20.0, 10, "tap-1")
        assertEquals(first, controller.completeSet(exercise, 20.0, 10, "tap-1"))
        controller.completeSet(exercise, 20.0, 10, "tap-2")
        val sets = controller.resumeActive()!!.exercises.first().sets
        assertEquals(2, sets.size)
        assertEquals(listOf(1, 2), sets.map { it.sequence })
        assertEquals(20.0, sets.first().weightKg!!, 0.0)
        assertEquals(10, sets.first().reps)
    }

    @Test fun concurrentStartAndSaveRequestsAreSerialized() = runBlocking {
        val starts = (1..2).map { async(Dispatchers.Default) { SessionController(db).start(1) } }.awaitAll()
        assertEquals(starts.first(), starts.last())
        val exercise = controller.resumeActive()!!.exercises.first().exercise.id
        val saves = (1..2).map { async(Dispatchers.Default) { SessionController(db).completeSet(exercise, 2.5, 1, "same-intent") } }.awaitAll()
        assertEquals(saves.first(), saves.last())
        assertEquals(1, controller.resumeActive()!!.exercises.first().sets.size)
    }

    @Test fun undoIsIdempotentAndNextSetDoesNotOverwriteRemainingRecords() = runBlocking {
        controller.start(1)
        val exercise = controller.resumeActive()!!.exercises.first().exercise.id
        val first = controller.completeSet(exercise, 20.0, 10)
        controller.completeSet(exercise, 22.5, 8)
        controller.undoSet(first)
        controller.undoSet(first)
        controller.completeSet(exercise, 25.0, 6)
        val sets = controller.resumeActive()!!.exercises.first().sets
        assertEquals(listOf(22.5, 25.0), sets.map { it.weightKg })
        assertEquals(listOf(2, 3), sets.map { it.sequence })
    }

    @Test fun pauseAndRecreateControllerPreserveRecordsAndCurrentExercise() = runBlocking {
        val session = controller.start(1)
        val exercise = controller.resumeActive()!!.exercises.first().exercise.id
        controller.completeSet(exercise, 27.5, 11)
        controller.moveTo(session, 2)
        controller.pause(session)
        val restored = SessionController(db).resumeActive()!!
        assertEquals(SessionStatus.PAUSED, restored.session.status)
        assertEquals(2, restored.session.currentPosition)
        assertEquals(27.5, restored.exercises.first().sets.single().weightKg!!, 0.0)
        controller.resume(session)
        assertEquals(SessionStatus.ACTIVE, controller.resumeActive()!!.session.status)
    }

    @Test fun previousCompletedSessionIsSuggestionOnlyAndSharedAcrossMenus() = runBlocking {
        val old = controller.start(1)
        val lateral = controller.resumeActive()!!.exercises[3].exercise.id
        controller.completeSet(lateral, 12.5, 12)
        controller.finish(old)
        controller.start(2)
        val workout = controller.resumeActive()!!
        val suggestion = workout.exercises[2].previous!!
        assertEquals(12.5, suggestion.weightKg!!, 0.0)
        assertEquals(12, suggestion.reps)
        assertTrue(workout.exercises.all { it.sets.isEmpty() })
        // Merely reading a suggestion, including after reconstruction, never creates a record.
        assertTrue(SessionController(db).resumeActive()!!.exercises.all { it.sets.isEmpty() })
    }

    @Test fun finishKeepsRecordsAndRejectsFurtherMutations() = runBlocking {
        val id = controller.start(1)
        val exercise = controller.resumeActive()!!.exercises.first().exercise.id
        val record = controller.completeSet(exercise, 20.0, 10)
        controller.finish(id)
        controller.finish(id)
        assertNull(controller.resumeActive())
        assertEquals(SessionStatus.COMPLETED, db.workoutSessionDao().session(id)!!.status)
        assertNotNull(db.workoutSessionDao().record(record))
        expectInvalid { controller.completeSet(exercise, 20.0, 10) }
        expectInvalid { controller.undoSet(record) }
        assertTrue(controller.start(2) != id)
    }

    @Test fun invalidInputsAndUnknownIdsLeaveDatabaseUnchanged() = runBlocking {
        expectInvalid { controller.start(999) }
        assertNull(controller.resumeActive())
        val id = controller.start(1)
        val exercise = controller.resumeActive()!!.exercises.first().exercise.id
        for (weight in listOf(0.0, -0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            expectInvalid { controller.completeSet(exercise, weight, 10) }
        }
        expectInvalid { controller.completeSet(exercise, 20.0, 0) }
        expectInvalid { controller.completeSet(999, 20.0, 10) }
        expectInvalid { controller.moveTo(id, 5) }
        assertTrue(controller.resumeActive()!!.exercises.all { it.sets.isEmpty() })
        controller.completeSet(exercise, 0.5, 1)
        assertEquals(1, controller.resumeActive()!!.exercises.first().sets.size)
    }

    @Test fun editFirstSetKeepsOrderIdentityAndOtherSets() = runBlocking {
        val session = controller.start(1)
        val exercise = controller.resumeActive()!!.exercises.first().exercise.id
        repeat(3) { controller.completeSet(exercise, 20.0 + it * 2.5, 10 - it) }
        val before = db.workoutSessionDao().records(exercise)
        controller.updateSet(before.first().id, 17.5, 12)
        val after = db.workoutSessionDao().records(exercise)
        assertEquals(before.map { it.id }, after.map { it.id })
        assertEquals(before.first().copy(weightKg = 17.5, reps = 12), after.first())
        assertEquals(before.drop(1), after.drop(1))
        controller.pause(session)
        controller.resume(session)
        assertEquals(after, controller.resumeActive()!!.exercises.first().sets)
    }

    @Test fun invalidOrInactiveSetEditsDoNotChangeRecords() = runBlocking {
        val session = controller.start(1)
        val exercise = controller.resumeActive()!!.exercises.first().exercise.id
        val id = controller.completeSet(exercise, 20.0, 10)
        val before = db.workoutSessionDao().record(id)
        for (weight in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            expectInvalid { controller.updateSet(id, weight, 10) }
        }
        expectInvalid { controller.updateSet(id, 20.0, 0) }
        expectInvalid { controller.updateSet(999, 20.0, 10) }
        controller.pause(session)
        expectInvalid { controller.updateSet(id, 25.0, 12) }
        controller.resume(session)
        controller.finish(session)
        expectInvalid { controller.updateSet(id, 25.0, 12) }
        assertEquals(before, db.workoutSessionDao().record(id))
    }

    private suspend fun expectInvalid(action: suspend () -> Unit) {
        try { action(); fail("Expected validation failure") } catch (_: IllegalArgumentException) { }
    }
}
