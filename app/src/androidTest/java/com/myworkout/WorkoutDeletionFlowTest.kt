package com.myworkout

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.myworkout.data.*
import com.myworkout.domain.SessionController
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDeletionFlowTest {
    @Test fun cancelKeepsWorkoutThenConfirmedDeleteRemovesOnlySelectedHistoryAndPersists() {
        val db=AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        val c=SessionController(db);val ids=mutableListOf<Long>()
        val pair=runBlocking {
            val repo=WorkoutRepository(db);repo.prepareHome()
            val routine=repo.observeHomeRoutines().first().first().routine.id
            for(name in listOf("削除対象テスト","保持対象テスト")) {
                val id=c.start(routine);ids.add(id)
                c.completeSet(c.resumeActive()!!.exercises.first().exercise.id,null,8);c.finish(id)
                db.openHelper.writableDatabase.execSQL("UPDATE workout_sessions SET routineName=? WHERE id=?",arrayOf<Any>(name,id))
            }
            ids[0] to ids[1]
        }
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());val actions=UiActions(device)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                actions.click("履歴")
                actions.click(By.desc("ワークアウト${pair.first}を削除"))
                actions.visible("ワークアウトを削除しますか？")
                actions.find(By.textContains("削除対象テスト"))
                actions.click("キャンセル")
                runBlocking { assertNotNull(db.workoutSessionDao().session(pair.first)) }
                actions.click(By.desc("ワークアウト${pair.first}を削除"))
                actions.click("削除する")
                actions.visible("トレーニング履歴")
                assertFalse(device.hasObject(By.textContains("削除対象テスト")))
                actions.find(By.textContains("保持対象テスト"))
                runBlocking {
                    assertNull(db.workoutSessionDao().session(pair.first))
                    assertTrue(db.workoutSessionDao().exercises(pair.first).isEmpty())
                    assertNotNull(db.workoutSessionDao().session(pair.second))
                }
                actions.click("閉じる")
                scenario.recreate()
                actions.click("履歴")
                assertFalse(device.hasObject(By.desc("ワークアウト${pair.first}を削除")))
                actions.find(By.textContains("保持対象テスト"))
                // A restored confirmation ID must not hide history after its target disappears.
                actions.click(By.desc("ワークアウト${pair.second}を削除"))
                actions.visible("ワークアウトを削除しますか？")
                runBlocking { WorkoutHistoryRepository(db).deleteCompleted(pair.second) }
                actions.visible("トレーニング履歴")
                scenario.recreate()
                actions.click("履歴")
                actions.visible("トレーニング履歴")
                assertFalse(device.hasObject(By.text("ワークアウトを削除しますか？")))
            }
        } finally {runBlocking {ids.forEach{db.openHelper.writableDatabase.execSQL("DELETE FROM workout_sessions WHERE id=?",arrayOf(it))}}}
    }
}
