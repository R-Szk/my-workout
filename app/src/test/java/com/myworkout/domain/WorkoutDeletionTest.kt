package com.myworkout.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.myworkout.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
class WorkoutDeletionTest {
    private lateinit var db:AppDatabase
    private lateinit var c:SessionController
    private lateinit var repo:WorkoutHistoryRepository
    @Before fun setup()=runBlocking { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).build();WorkoutRepository(db).seedIfEmpty();c=SessionController(db);repo=WorkoutHistoryRepository(db) }
    @After fun close(){db.close()}
    private suspend fun completed(reps:Int):Long {val id=c.start(1);c.completeSet(c.resumeActive()!!.exercises.first().exercise.id,20.0,reps);c.finish(id);return id}
    @Test fun deletingOneWorkoutCascadesOnlyItsExercisesAndSetsAndRefreshesPrevious()=runBlocking {
        val first=completed(8);val latest=completed(10)
        val exercises=db.workoutSessionDao().exercises(latest);val record=db.workoutSessionDao().records(exercises.first().id).single()
        c.start(1)
        assertEquals(10,c.resumeActive()!!.exercises.first().previous!!.reps)
        assertTrue(repo.deleteCompleted(latest))
        assertNull(repo.session(latest));assertTrue(db.workoutSessionDao().exercises(latest).isEmpty());assertNull(db.workoutSessionDao().record(record.id))
        assertEquals(listOf(first),repo.observeHistory().first().map{it.session.id})
        assertEquals(8,c.resumeActive()!!.exercises.first().previous!!.reps)
        assertNotNull(c.resumeActive());assertEquals(9,WorkoutRepository(db).observeMachines().first().size)
        assertFalse(repo.deleteCompleted(latest))
    }
    @Test fun unfinishedWorkoutCannotBeDeletedFromHistory()=runBlocking {
        val id=c.start(1);val record=c.completeSet(c.resumeActive()!!.exercises.first().exercise.id,20.0,8)
        assertFalse(repo.deleteCompleted(id));c.pause(id);assertFalse(repo.deleteCompleted(id))
        assertNotNull(db.workoutSessionDao().record(record));assertEquals(id,c.resumeActive()!!.session.id)
    }
    @Test fun deletingLastWorkoutEmptiesHistoryAndTrainingCalendar()=runBlocking {
        val id=completed(8);assertTrue(repo.deleteCompleted(id))
        val history=repo.observeHistory().first();assertTrue(history.isEmpty());assertTrue(ProgressCalculator.trainingDays(history).isEmpty())
        c.start(1);assertNull(c.resumeActive()!!.exercises.first().previous)
    }
}
