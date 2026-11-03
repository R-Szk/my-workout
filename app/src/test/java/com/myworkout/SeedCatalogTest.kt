package com.myworkout

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.myworkout.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SeedCatalogTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun menusMatchDesignAndAreOrdered() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = WorkoutRepository(db)
            repository.seedIfEmpty()
            val a = repository.observeRoutine(1).first()!!
            val b = repository.observeRoutine(2).first()!!
            assertEquals("A：胸・肩・背中", a.routine.name)
            assertEquals("B：胸・背中・腕・脚", b.routine.name)
            assertEquals(listOf("チェストプレス", "ラットプルダウン", "ショルダープレス", "ラテラルレイズ", "ディップ・チンアシスト"), a.exercises.map { it.machine.name })
            assertEquals(listOf("バーティカルインクラインベンチプレス", "シーテッドロー", "ラテラルレイズ", "プリチャーカール", "レッグプレス"), b.exercises.map { it.machine.name })
            for (routine in listOf(a, b)) {
                assertEquals(listOf(0, 1, 2, 3, 4), routine.exercises.map { it.exercise.position })
                assertEquals(listOf(3, 3, 2, 2, 2), routine.exercises.map { it.exercise.targetSets })
            }
            assertEquals(listOf(10, 10, 10, 12, 10), a.exercises.map { it.exercise.targetReps })
            assertEquals(listOf(10, 10, 12, 10, 10), b.exercises.map { it.exercise.targetReps })
            assertEquals(9, repository.observeMachines().first().size)
            assertNull(repository.observeRoutine(999).first())
        } finally { db.close() }
    }

    @Test fun seedingTwiceAndReopeningPreservesCatalogAndEdits() = runBlocking {
        val name = "seed-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            WorkoutRepository(db).seedIfEmpty()
            db.openHelper.writableDatabase.execSQL("UPDATE machines SET name = '自分のマシン' WHERE id = 1")
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val repository = WorkoutRepository(db)
            repository.seedIfEmpty()
            assertEquals(9, repository.observeMachines().first().size)
            assertEquals("自分のマシン", repository.observeMachines().first().first { it.id == 1L }.name)
            assertEquals(5, repository.observeRoutine(1).first()!!.exercises.size)
            assertEquals(5, repository.observeRoutine(2).first()!!.exercises.size)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
