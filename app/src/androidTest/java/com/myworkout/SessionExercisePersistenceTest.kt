package com.myworkout

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.myworkout.data.*
import com.myworkout.domain.SessionController
import com.myworkout.domain.SessionExerciseEditor
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionExercisePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun migrateV2ThenEditAndReopenPreservesOldSetsHistoryAndTodaysQueue() = runBlocking {
        val name = "exercise-editor-migration-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.myworkout.data.AppDatabase/2.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                for (i in 0 until (indices?.length() ?: 0)) old.execSQL(indices!!.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val queries = schema.getJSONArray("setupQueries")
            for (i in 0 until queries.length()) old.execSQL(queries.getString(i))
            SeedCatalog.machines.forEach { old.execSQL("INSERT INTO machines (id,name,imageAsset,notes) VALUES (?,?,?,?)", arrayOf<Any?>(it.id,it.name,it.imageAsset,it.notes)) }
            SeedCatalog.routines.forEach { old.execSQL("INSERT INTO routines (id,name) VALUES (?,?)", arrayOf<Any?>(it.id,it.name)) }
            SeedCatalog.exercises.forEach { old.execSQL("INSERT INTO routine_exercises VALUES (?,?,?,?,?)", arrayOf<Any?>(it.routineId,it.machineId,it.position,it.targetSets,it.targetReps)) }
            old.execSQL("INSERT INTO workout_sessions (id,routineId,routineName,startedAt,endedAt,status,currentPosition,activeSlot) VALUES (1,1,'A：胸・肩・背中',100,NULL,'ACTIVE',1,1)")
            old.execSQL("INSERT INTO workout_sessions (id,routineId,routineName,startedAt,endedAt,status,currentPosition,activeSlot) VALUES (2,1,'過去のA',10,20,'COMPLETED',0,NULL)")
            old.execSQL("INSERT INTO session_exercises (id,sessionId,machineId,machineName,notes,position,targetSets,targetReps) VALUES (1,1,1,'チェストプレス','',0,3,10)")
            old.execSQL("INSERT INTO session_exercises (id,sessionId,machineId,machineName,notes,position,targetSets,targetReps) VALUES (2,1,2,'ラットプルダウン','',1,3,10)")
            old.execSQL("INSERT INTO session_exercises (id,sessionId,machineId,machineName,notes,position,targetSets,targetReps) VALUES (3,2,1,'以前のチェストプレス','',0,2,8)")
            old.execSQL("INSERT INTO set_records (id,sessionExerciseId,sequence,weightKg,reps,completedAt,requestId) VALUES (1,1,1,42.5,7,101,'old-active')")
            old.execSQL("INSERT INTO set_records (id,sessionExerciseId,sequence,weightKg,reps,completedAt,requestId) VALUES (2,3,1,20,8,15,'old-completed')")
            old.version = 2
        }
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_4_5).build()
        try {
            val controller = SessionController(db)
            val migrated = controller.resumeActive()!!
            assertTrue(migrated.exercises.all { it.exercise.status == ExerciseStatus.PENDING })
            assertEquals(1, migrated.session.currentPosition)
            assertEquals(42.5, migrated.exercises.first().sets.single().weightKg!!, 0.0)
            val historicalExercises = db.workoutSessionDao().exercises(2)
            val historicalSet = db.workoutSessionDao().record(2)
            val editor = SessionExerciseEditor(db)
            editor.reorder(1, listOf(2, 1))
            editor.skip(2)
            val added = editor.add(1, 8)
            val edited = controller.resumeActive()!!
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_4_5).build()
            val restored = SessionController(db).resumeActive()!!
            assertEquals(edited, restored)
            assertEquals(listOf(2L, 1L, added), restored.exercises.map { it.exercise.id })
            assertEquals(ExerciseStatus.SKIPPED, restored.exercises.first().exercise.status)
            assertEquals(1, restored.session.currentPosition)
            assertEquals(42.5, restored.exercises[1].sets.single().weightKg!!, 0.0)
            assertEquals(historicalExercises, db.workoutSessionDao().exercises(2))
            assertEquals(historicalSet, db.workoutSessionDao().record(2))
            SessionController(db).finish(1)
            assertEquals(ExerciseStatus.SKIPPED, db.workoutSessionDao().exercise(2)!!.status)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
