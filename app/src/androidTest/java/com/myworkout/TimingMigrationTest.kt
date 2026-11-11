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
class TimingMigrationTest {
    @Test fun v5HistoryAndPausedRecordsSurviveWithoutInventingElapsedTime() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="timing-migration-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("com.myworkout.data.AppDatabase/5.json").bufferedReader().use { it.readText() }).getJSONObject("database")
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
            old.execSQL("INSERT INTO workout_sessions(id,routineId,routineName,startedAt,endedAt,status,currentPosition,activeSlot,routineKey) VALUES(1,1001,'自宅A',1000,100000,'COMPLETED',0,NULL,'home.A')")
            old.execSQL("INSERT INTO workout_sessions(id,routineId,routineName,startedAt,endedAt,status,currentPosition,activeSlot) VALUES(2,1,'空の旧記録',1000,100000,'COMPLETED',0,NULL)")
            old.execSQL("INSERT INTO workout_sessions(id,routineId,routineName,startedAt,status,currentPosition,activeSlot,routineKey) VALUES(3,1001,'中断中A',1000,'PAUSED',0,1,'home.A')")
            for(id in listOf(1,3)) {
                old.execSQL("INSERT INTO session_exercises(id,sessionId,machineId,machineName,notes,position,targetSets,targetReps,status,visualKey,loadType) VALUES(?,?,1001,'プッシュアップ','',0,2,8,'PENDING','home.push_up','BODYWEIGHT')",arrayOf(id,id))
                old.execSQL("INSERT INTO set_records VALUES(?,?,1,NULL,8,2000,?)",arrayOf<Any>(id,id,"old-$id"))
            }
            old.version=5
        }
        val db=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_4_5).build()
        try {
            val history=WorkoutHistoryRepository(db).observeHistory().first()
            assertEquals(listOf(1L),history.map{it.session.id})
            assertNull(com.myworkout.domain.ProgressCalculator.summary(history.single(),history).durationMillis)
            val c=SessionController(db)
            assertEquals(SessionStatus.PAUSED,c.resumeActive()!!.session.status)
            c.resume(3);assertTrue(c.finish(3))
            assertNull(db.workoutSessionDao().session(3)!!.activeDurationMillis)
            assertEquals(8,db.workoutSessionDao().records(3).single().reps)
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use{assertFalse(it.moveToFirst())}
        } finally {db.close();context.deleteDatabase(name)}
    }
}
