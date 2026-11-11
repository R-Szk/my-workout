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
class WorkoutTimingTest {
    private lateinit var db:AppDatabase
    private var time=1000L
    private fun controller()=SessionController(db,now={time})
    @Before fun setup()=runBlocking { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).build();WorkoutRepository(db).seedIfEmpty() }
    @After fun close(){db.close()}
    @Test fun emptyAndFullyUndoneWorkoutsAreNotHistory()=runBlocking {
        val c=controller();val id=c.start(1)
        assertFalse(c.finish(id));assertTrue(WorkoutHistoryRepository(db).observeHistory().first().isEmpty())
        val second=c.start(1);val record=c.completeSet(c.resumeActive()!!.exercises.first().exercise.id,20.0,10)
        c.undoSet(record);assertFalse(c.finish(second));assertTrue(WorkoutHistoryRepository(db).observeHistory().first().isEmpty());assertNull(c.resumeActive())
    }
    @Test fun persistedTimerExcludesMultiplePausesAndRepeatedResumeDoesNotResetIt()=runBlocking {
        var c=controller();val id=c.start(1);c.completeSet(c.resumeActive()!!.exercises.first().exercise.id,20.0,10)
        time=11000;c.pause(id);time=21000;c.pause(id)
        time=31000;c=controller();c.resume(id);time=36000;c.resume(id)
        time=41000;c.pause(id);time=91000;c.resume(id)
        time=96000;assertTrue(c.finish(id));time=200000;c.finish(id)
        val history=WorkoutHistoryRepository(db).observeHistory().first()
        assertEquals(25000L,ProgressCalculator.summary(history.single(),history).durationMillis)
    }
    @Test fun finishWhilePausedDoesNotCountPause()=runBlocking {
        val c=controller();val id=c.start(1);c.completeSet(c.resumeActive()!!.exercises.first().exercise.id,20.0,8)
        time=6000;c.pause(id);time=600000;c.finish(id)
        val history=WorkoutHistoryRepository(db).observeHistory().first()
        assertEquals(5000L,ProgressCalculator.summary(history.single(),history).durationMillis)
    }
}
