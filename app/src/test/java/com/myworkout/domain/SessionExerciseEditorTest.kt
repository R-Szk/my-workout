package com.myworkout.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.myworkout.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionExerciseEditorTest {
    private lateinit var db: AppDatabase
    private lateinit var controller: SessionController
    private lateinit var editor: SessionExerciseEditor
    private var session = 0L

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        WorkoutRepository(db).seedIfEmpty()
        controller = SessionController(db)
        editor = SessionExerciseEditor(db)
        session = controller.start(1)
    }
    @After fun close() { db.close() }

    @Test fun reorderPreservesCurrentExerciseAndSetsAndDoesNotEditTemplate() = runBlocking {
        val before = controller.resumeActive()!!
        val ids = before.exercises.map { it.exercise.id }
        val setId = controller.completeSet(ids[1], 30.0, 8)
        controller.moveTo(session, 1)
        val order = listOf(ids[4], ids[2], ids[0], ids[1], ids[3])
        editor.reorder(session, order)
        val after = controller.resumeActive()!!
        assertEquals(order, after.exercises.map { it.exercise.id })
        assertEquals(listOf(0, 1, 2, 3, 4), after.exercises.map { it.exercise.position })
        assertEquals(3, after.session.currentPosition)
        assertEquals(setId, after.exercises[3].sets.single().id)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), WorkoutRepository(db).observeRoutine(1).first()!!.exercises.map { it.machine.id })
    }

    @Test fun invalidPermutationNeverPartiallyChangesQueue() = runBlocking {
        val before = controller.resumeActive()!!
        val ids = before.exercises.map { it.exercise.id }
        for (order in listOf(emptyList(), ids.dropLast(1), ids + ids.first(), listOf(ids[0], ids[0], ids[2], ids[3], ids[4]), ids.dropLast(1) + 999L)) {
            expectInvalid { editor.reorder(session, order) }
            assertEquals(before, controller.resumeActive())
        }
    }

    @Test fun reorderRollsBackPositionsIfDatabaseWriteFails() = runBlocking {
        val before = controller.resumeActive()!!
        val ids = before.exercises.map { it.exercise.id }
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER reject_reorder BEFORE UPDATE OF position ON session_exercises
            WHEN NEW.id = ${ids.first()} AND NEW.position = 4 BEGIN SELECT RAISE(ABORT, 'test write failure'); END""")
        try { editor.reorder(session, ids.reversed()); fail("Expected SQLite write failure") } catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(before, controller.resumeActive())
    }

    @Test fun skipIsUnperformedMovesForwardAndCanBeRestored() = runBlocking {
        val first = controller.resumeActive()!!.exercises.first().exercise.id
        editor.skip(first)
        val skipped = controller.resumeActive()!!
        assertEquals(ExerciseStatus.SKIPPED, skipped.exercises.first().exercise.status)
        assertTrue(skipped.exercises.first().sets.isEmpty())
        assertEquals(1, skipped.session.currentPosition)
        editor.skip(first)
        assertEquals(1, controller.resumeActive()!!.session.currentPosition)
        expectInvalid { controller.completeSet(first, 20.0, 10) }
        editor.restore(first)
        assertEquals(ExerciseStatus.PENDING, controller.resumeActive()!!.exercises.first().exercise.status)
        controller.completeSet(first, 20.0, 10)
        assertEquals(1, controller.resumeActive()!!.exercises.first().sets.size)
    }

    @Test fun cannotMarkRecordedExerciseAsUnperformed() = runBlocking {
        val first = controller.resumeActive()!!.exercises.first().exercise.id
        val record = controller.completeSet(first, 20.0, 10)
        expectInvalid { editor.skip(first) }
        assertEquals(ExerciseStatus.PENDING, controller.resumeActive()!!.exercises.first().exercise.status)
        assertNotNull(db.workoutSessionDao().record(record))
        controller.undoSet(record)
        editor.skip(first)
        assertEquals(ExerciseStatus.SKIPPED, controller.resumeActive()!!.exercises.first().exercise.status)
    }

    @Test fun addAppendsSnapshotWithTargetsAndAllowsIntentionalRepeatedMachine() = runBlocking {
        val original = controller.resumeActive()!!
        val first = editor.add(session, 4)
        val second = editor.add(session, 4)
        assertNotEquals(first, second)
        val after = controller.resumeActive()!!
        assertEquals(7, after.exercises.size)
        assertEquals(original.exercises, after.exercises.take(5))
        assertEquals(0, after.session.currentPosition)
        assertEquals(listOf(5, 6), after.exercises.takeLast(2).map { it.exercise.position })
        assertEquals(12, after.exercises.last().exercise.targetReps)
        assertEquals(2, after.exercises.last().exercise.targetSets)
        assertTrue(after.exercises.last().sets.isEmpty())
        assertEquals(5, WorkoutRepository(db).observeRoutine(1).first()!!.exercises.size)
        db.openHelper.writableDatabase.execSQL("UPDATE machines SET name = '変更済み' WHERE id = 4")
        assertEquals("ラテラルレイズ", controller.resumeActive()!!.exercises.last().exercise.machineName)
    }

    @Test fun allSkippedQueueStaysValidAndNewMachineCanContinueWorkout() = runBlocking {
        val ids = controller.resumeActive()!!.exercises.map { it.exercise.id }
        ids.forEach { editor.skip(it) }
        val skipped = controller.resumeActive()!!
        assertTrue(skipped.session.currentPosition in skipped.exercises.indices)
        assertTrue(skipped.exercises.all { it.exercise.status == ExerciseStatus.SKIPPED })
        val addedId = editor.add(session, 8)
        val after = controller.resumeActive()!!
        assertEquals(5, after.session.currentPosition)
        assertEquals(addedId, after.exercises.last().exercise.id)
        assertEquals("プリチャーカール", after.exercises.last().exercise.machineName)
        assertEquals(2, after.exercises.last().exercise.targetSets)
        assertEquals(10, after.exercises.last().exercise.targetReps)
        controller.completeSet(addedId, 10.0, 10)
        assertEquals(1, controller.resumeActive()!!.exercises.last().sets.size)
    }

    @Test fun editsToTodaysSessionNeverModifyCompletedWorkoutOrCatalog() = runBlocking {
        val oldIds = controller.resumeActive()!!.exercises.map { it.exercise.id }
        val record = controller.completeSet(oldIds[0], 35.0, 9)
        controller.finish(session)
        val historicalSession = db.workoutSessionDao().session(session)
        val historicalExercises = db.workoutSessionDao().exercises(session)
        val catalog = WorkoutRepository(db).observeRoutine(1).first()
        val today = controller.start(1)
        val ids = controller.resumeActive()!!.exercises.map { it.exercise.id }
        editor.reorder(today, ids.reversed())
        editor.skip(ids.last())
        editor.add(today, 9)
        assertEquals(historicalSession, db.workoutSessionDao().session(session))
        assertEquals(historicalExercises, db.workoutSessionDao().exercises(session))
        assertEquals(35.0, db.workoutSessionDao().record(record)!!.weightKg!!, 0.0)
        assertEquals(catalog, WorkoutRepository(db).observeRoutine(1).first())
        expectInvalid { editor.skip(oldIds[0]) }
        expectInvalid { editor.reorder(session, oldIds.reversed()) }
        expectInvalid { editor.add(session, 9) }
        expectInvalid { editor.restore(oldIds[0]) }
    }

    @Test fun pausedUnknownAndForeignRequestsDoNotModifySessions() = runBlocking {
        val ids = controller.resumeActive()!!.exercises.map { it.exercise.id }
        expectInvalid { editor.add(session, 999) }
        expectInvalid { editor.skip(999) }
        expectInvalid { editor.restore(999) }
        expectInvalid { editor.reorder(999, ids) }
        expectInvalid { editor.add(999, 1) }
        controller.pause(session)
        val before = controller.resumeActive()
        expectInvalid { editor.skip(ids[0]) }
        expectInvalid { editor.restore(ids[0]) }
        expectInvalid { editor.reorder(session, ids.reversed()) }
        expectInvalid { editor.add(session, 8) }
        assertEquals(before, controller.resumeActive())
    }

    @Test fun concurrentAdditionsReceiveDistinctIdsAndContinuousPositions() = runBlocking {
        val added = (1..3).map { async(Dispatchers.Default) { SessionExerciseEditor(db).add(session, 8) } }.awaitAll()
        assertEquals(3, added.toSet().size)
        assertEquals((0..7).toList(), controller.resumeActive()!!.exercises.map { it.exercise.position })
    }

    private suspend fun expectInvalid(action: suspend () -> Unit) {
        try { action(); fail("Expected validation failure") } catch (_: IllegalArgumentException) { }
    }
}
