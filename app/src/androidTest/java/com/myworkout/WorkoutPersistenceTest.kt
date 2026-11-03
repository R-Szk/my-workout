package com.myworkout

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.myworkout.data.*
import com.myworkout.domain.SessionController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun reopenRestoresSavedSetsPositionAndPauseWithoutDuplicate() = runBlocking {
        val name = "workout-persistence-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            WorkoutRepository(db).seedIfEmpty()
            val controller = SessionController(db)
            val sessionId = controller.start(1)
            val exerciseId = controller.resumeActive()!!.exercises.first().exercise.id
            val setId = controller.completeSet(exerciseId, 32.5, 9, "persistent-intent")
            controller.moveTo(sessionId, 1)
            controller.pause(sessionId)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val restored = SessionController(db)
            val workout = restored.resumeActive()!!
            assertEquals(sessionId, workout.session.id)
            assertEquals(SessionStatus.PAUSED, workout.session.status)
            assertEquals(1, workout.session.currentPosition)
            assertEquals(setId, workout.exercises.first().sets.single().id)
            assertEquals(32.5, workout.exercises.first().sets.single().weightKg, 0.0)
            restored.resume(sessionId)
            assertEquals(setId, restored.completeSet(exerciseId, 32.5, 9, "persistent-intent"))
            assertEquals(1, restored.resumeActive()!!.exercises.first().sets.size)
            restored.finish(sessionId)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertNull(SessionController(db).resumeActive())
            assertEquals(1, db.workoutSessionDao().records(exerciseId).size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migrationFromStageOnePreservesEditedCatalogAndAddsWorkoutTables() = runBlocking {
        val name = "workout-migration-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.myworkout.data.AppDatabase/1.json").bufferedReader().use { it.readText() }).getJSONObject("database")
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
            old.execSQL("INSERT INTO machines VALUES (1, '自分のチェストプレス', NULL, '編集済み')")
            old.execSQL("INSERT INTO routines VALUES (1, '自分のA')")
            old.execSQL("INSERT INTO routine_exercises VALUES (1, 1, 0, 2, 8)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            WorkoutRepository(db).seedIfEmpty()
            val routine = WorkoutRepository(db).observeRoutine(1).first()!!
            assertEquals("自分のA", routine.routine.name)
            assertEquals("自分のチェストプレス", routine.exercises.single().machine.name)
            assertEquals(2, routine.exercises.single().exercise.targetSets)
            val controller = SessionController(db)
            controller.start(1)
            val exercise = controller.resumeActive()!!.exercises.single().exercise
            assertEquals(8, exercise.targetReps)
            controller.completeSet(exercise.id, 10.0, 8)
            assertEquals(1, controller.resumeActive()!!.exercises.single().sets.size)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
