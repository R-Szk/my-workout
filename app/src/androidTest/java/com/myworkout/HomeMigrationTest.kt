package com.myworkout

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.myworkout.data.*
import com.myworkout.domain.SessionController
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeMigrationTest {
    @Test fun v4EditedCatalogAndLegacyZeroWeightsSurviveAndHomeCanSaveNull() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="home-migration-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("com.myworkout.data.AppDatabase/4.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name),null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity=entities.getJSONObject(index);val table=entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",table))
                val indices=entity.optJSONArray("indices")
                for(i in 0 until (indices?.length() ?: 0)) old.execSQL(indices!!.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}",table))
            }
            val queries=schema.getJSONArray("setupQueries")
            for(i in 0 until queries.length()) old.execSQL(queries.getString(i))
            old.execSQL("INSERT INTO machines VALUES(1,'編集済みチェスト',NULL,'自分の説明','chest_press')")
            old.execSQL("INSERT INTO routines VALUES(1,'編集済みA')")
            old.execSQL("INSERT INTO routine_exercises VALUES(1,1,0,4,11)")
            old.execSQL("INSERT INTO workout_sessions(id,routineId,routineName,startedAt,status,currentPosition,activeSlot) VALUES(1,1,'旧A',100,'PAUSED',1,1)")
            old.execSQL("INSERT INTO session_exercises(id,sessionId,machineId,machineName,notes,position,targetSets,targetReps,status,visualKey) VALUES(1,1,1,'旧チェスト','保持',0,4,11,'PENDING','chest_press')")
            old.execSQL("INSERT INTO session_exercises(id,sessionId,machineId,machineName,notes,position,targetSets,targetReps,status,visualKey) VALUES(2,1,1,'未実施','',1,2,10,'SKIPPED','chest_press')")
            old.execSQL("INSERT INTO set_records VALUES(1,1,1,0,10,101,'old-zero')")
            old.execSQL("INSERT INTO set_records VALUES(100,1,2,20,9,102,'deleted')")
            old.execSQL("DELETE FROM set_records WHERE id=100")
            old.version=4
        }
        val db=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_4_5).build()
        try {
            val controller=SessionController(db)
            val active=controller.resumeActive()!!
            assertEquals(SessionStatus.PAUSED,active.session.status)
            assertEquals(1,active.session.currentPosition)
            assertEquals(ExerciseStatus.SKIPPED,active.exercises[1].exercise.status)
            assertEquals(0.0,active.exercises[0].sets.single().weightKg!!,0.0)
            assertEquals("old-zero",active.exercises[0].sets.single().requestId)
            assertNull(active.exercises[0].exercise.loadType)
            val repo=WorkoutRepository(db);repo.prepareHome();repo.prepareHome()
            assertEquals("編集済みチェスト",repo.observeMachines().first().first{it.id==1L}.name)
            assertEquals(4,repo.observeRoutine(1).first()!!.exercises.single().exercise.targetSets)
            controller.resume(1)
            val next=controller.completeSet(1,22.5,12)
            assertTrue(next>100)
            controller.finish(1)
            controller.start(repo.observeHomeRoutines().first().first().routine.id)
            val home=controller.resumeActive()!!.exercises.first().exercise.id
            controller.completeSet(home,null,8)
            assertNull(db.workoutSessionDao().records(home).single().weightKg)
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally { db.close();context.deleteDatabase(name) }
    }
}
