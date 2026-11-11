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
class HomeRecordingTest {
    private lateinit var db:AppDatabase
    private lateinit var repo:WorkoutRepository
    private lateinit var controller:SessionController
    @Before fun setup() { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).build();repo=WorkoutRepository(db);controller=SessionController(db) }
    @After fun close() { db.close() }
    private suspend fun home()=repo.observeHomeRoutines().first()
    @Test fun freshInstallHasOnlySixHomeExercisesAndNeverOverwritesTargets() = runBlocking {
        repo.prepareHome()
        val menus=home()
        assertEquals(listOf("A：胸・肩・二の腕","B：背中・力こぶ・下半身"),menus.map{it.routine.name})
        assertEquals(listOf("home.push_up","home.lateral_raise","home.triceps_extension","home.one_arm_row","home.dumbbell_curl","home.squat"),menus.flatMap{it.exercises}.map{it.machine.visualKey})
        assertEquals(6,repo.observeMachines().first().size)
        assertTrue(menus.flatMap{it.exercises}.all{it.exercise.targetSets==2})
        db.openHelper.writableDatabase.execSQL("UPDATE routine_exercises SET targetSets=1 WHERE routineId=? AND position=0",arrayOf(menus[0].routine.id))
        repo.prepareHome()
        assertEquals(6,repo.observeMachines().first().size)
        assertEquals(1,home()[0].exercises[0].exercise.targetSets)
    }
    @Test fun oldEditedCatalogAndPausedSessionArePreserved() = runBlocking {
        repo.seedIfEmpty()
        val session=controller.start(1)
        val exercise=controller.resumeActive()!!.exercises[0].exercise.id
        controller.completeSet(exercise,20.0,10)
        controller.pause(session)
        db.openHelper.writableDatabase.execSQL("UPDATE machines SET name='自分のチェスト' WHERE id=1")
        repo.prepareHome();repo.prepareHome()
        assertEquals(15,repo.observeMachines().first().size)
        assertEquals("自分のチェスト",repo.observeMachines().first().first{it.id==1L}.name)
        assertEquals(session,controller.resumeActive()!!.session.id)
        assertEquals(SessionStatus.PAUSED,controller.resumeActive()!!.session.status)
        assertEquals(20.0,controller.resumeActive()!!.exercises[0].sets.single().weightKg!!,0.0)
    }
    @Test fun bodyweightCanSaveEditResumeAndReusePreviousRepsWithoutWeight() = runBlocking {
        repo.prepareHome();val a=home()[0].routine.id
        val session=controller.start(a);val id=controller.resumeActive()!!.exercises[0].exercise.id
        val set=controller.completeSet(id,null,8,"body-one")
        assertEquals(set,controller.completeSet(id,null,8,"body-one"))
        controller.completeSet(id,null,9)
        controller.updateSet(set,null,10)
        assertEquals(listOf(10,9),controller.resumeActive()!!.exercises[0].sets.map{it.reps})
        assertTrue(controller.resumeActive()!!.exercises[0].sets.all{it.weightKg==null})
        controller.pause(session);controller.resume(session)
        assertEquals(2,controller.resumeActive()!!.exercises[0].sets.size)
        controller.finish(session);controller.start(a)
        assertEquals(9,controller.resumeActive()!!.exercises[0].previous!!.reps)
        assertNull(controller.resumeActive()!!.exercises[0].previous!!.weightKg)
    }
    @Test fun homeDumbbellsRequirePositiveWeightAndStaySeparateFromGymHistory() = runBlocking {
        repo.prepareHome();val session=controller.start(home()[1].routine.id)
        controller.moveTo(session,1)
        val id=controller.resumeActive()!!.exercises[1].exercise.id
        for(weight in listOf(null,0.0,-1.0,Double.NaN)) {
            try { controller.completeSet(id,weight,10);fail("Invalid dumbbell weight") } catch(_:IllegalArgumentException){}
        }
        controller.completeSet(id,5.0,10)
        assertEquals(5.0,controller.resumeActive()!!.exercises[1].sets.single().weightKg!!,0.0)
        assertEquals(LoadType.EXTERNAL,VisualCatalog.loadTypeFor(controller.resumeActive()!!.exercises[1].exercise))
    }
    @Test fun bodyweightSummaryHasRepetitionProgressAndNoInventedWeight() = runBlocking {
        repo.prepareHome();val a=home()[0].routine.id
        val first=controller.start(a);controller.completeSet(controller.resumeActive()!!.exercises[0].exercise.id,null,8);controller.finish(first)
        val second=controller.start(a);controller.completeSet(controller.resumeActive()!!.exercises[0].exercise.id,null,10);controller.finish(second)
        val history=WorkoutHistoryRepository(db).observeHistory().first()
        val summary=ProgressCalculator.summary(history.first{it.session.id==second},history)
        assertEquals(1,summary.setCount);assertEquals(1,summary.exerciseCount)
        val progress=summary.progress.single()
        assertEquals(LoadType.BODYWEIGHT,progress.loadType)
        assertNull(progress.currentWeightKg);assertNull(progress.weightChange)
        assertEquals(2,progress.repetitionChange)
        assertTrue(progress.points.all{it.weightKg==null})
    }
    @Test fun explicitlySwitchFromPausedGymToSelectedHomePreservesOldSets() = runBlocking {
        repo.seedIfEmpty();repo.prepareHome()
        val old=controller.start(1)
        val exercise=controller.resumeActive()!!.exercises.first().exercise.id
        controller.completeSet(exercise,20.0,10);controller.pause(old)
        val selected=home()[1].routine.id
        val next=controller.finishAndStart(old,selected)
        val active=controller.resumeActive()!!
        assertNotEquals(old,next)
        assertEquals(selected,active.session.routineId)
        assertEquals("home.B",active.session.routineKey)
        assertEquals("ワンハンド・ダンベルロー",active.exercises.first().exercise.machineName)
        assertEquals(SessionStatus.COMPLETED,db.workoutSessionDao().session(old)!!.status)
        assertEquals(20.0,db.workoutSessionDao().records(exercise).single().weightKg!!,0.0)
        try { controller.finishAndStart(old,home()[0].routine.id);fail("Stale confirmation") } catch(_:IllegalArgumentException){}
        assertEquals(next,controller.resumeActive()!!.session.id)
    }
    @Test fun failedSwitchLeavesPausedWorkoutAndRecordsIntact() = runBlocking {
        repo.prepareHome();val old=controller.start(home()[0].routine.id)
        controller.completeSet(controller.resumeActive()!!.exercises.first().exercise.id,null,8)
        controller.pause(old)
        try { controller.finishAndStart(old,-1);fail("Missing routine") } catch(_:IllegalArgumentException){}
        val active=controller.resumeActive()!!
        assertEquals(old,active.session.id)
        assertEquals(SessionStatus.PAUSED,active.session.status)
        assertEquals(8,active.exercises.first().sets.single().reps)
    }

}
