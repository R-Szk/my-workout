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
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualMigrationTest {
    @Test fun v3RecordsPositionSkipAndUnknownVariantSurviveMigration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "visual-migration-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("com.myworkout.data.AppDatabase/3.json").bufferedReader().use { it.readText() }).getJSONObject("database")
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
            old.execSQL("INSERT INTO machines VALUES (5,'自分の複合機',NULL,'補助重量')")
            old.execSQL("INSERT INTO workout_sessions (id,routineId,routineName,startedAt,status,currentPosition,activeSlot) VALUES(1,1,'編集済みA',100,'PAUSED',1,1)")
            old.execSQL("INSERT INTO session_exercises (id,sessionId,machineId,machineName,notes,position,targetSets,targetReps,status) VALUES(1,1,5,'複合機','',0,2,10,'PENDING')")
            old.execSQL("INSERT INTO session_exercises (id,sessionId,machineId,machineName,notes,position,targetSets,targetReps,status) VALUES(2,1,5,'複合機','',1,2,10,'SKIPPED')")
            old.execSQL("INSERT INTO set_records VALUES(1,1,1,30,10,101,'before-upgrade')")
            old.version=3
        }
        val db=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
        try {
            val active=SessionController(db).resumeActive()!!
            assertEquals(1,active.session.currentPosition)
            assertEquals(SessionStatus.PAUSED,active.session.status)
            assertEquals(ExerciseStatus.SKIPPED,active.exercises[1].exercise.status)
            assertNull(active.exercises[0].exercise.visualKey)
            assertNull(VisualCatalog.resolve(active.exercises[0].exercise))
            assertEquals(30.0,active.exercises[0].sets.single().weightKg,0.0)
            assertEquals("自分の複合機",db.openHelper.readableDatabase.query("SELECT name FROM machines WHERE id=5").use { it.moveToFirst();it.getString(0) })
        } finally { db.close();context.deleteDatabase(name) }
    }
}
