package com.myworkout

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.myworkout.data.*
import com.myworkout.domain.SessionController
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuSelectionFlowTest {
    @Test fun selectedEnvironmentIsClearAndConfirmedSwitchStartsChosenMenuWithoutLosingSets() {
        val db=AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        val controller=SessionController(db)
        val ids=mutableListOf<Long>()
        LegacyUiFixture.ensure()
        val old=runBlocking {
            WorkoutRepository(db).prepareHome()
            val id=controller.start(1);ids.add(id)
            controller.completeSet(controller.resumeActive()!!.exercises.first().exercise.id,20.0,10)
            controller.pause(id);id
        }
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());val actions=UiActions(device)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                assertFalse(device.hasObject(By.textStartsWith("表示中：")))
                actions.visible("中断中：マシントレーニング")
                actions.click("マシントレーニング")
                assertFalse(device.hasObject(By.textStartsWith("表示中：")))
                actions.click("自重トレーニング")
                assertFalse(device.hasObject(By.textStartsWith("表示中：")))
                actions.click("B：背中・力こぶ・下半身")
                actions.click("選択したメニューで開始")
                actions.visible("中断中の記録を保存して切り替えますか？")
                actions.click("キャンセル")
                runBlocking { assertEquals(old,controller.resumeActive()!!.session.id);assertEquals(SessionStatus.PAUSED,controller.resumeActive()!!.session.status) }
                actions.click("選択したメニューで開始")
                actions.click("保存して開始")
                actions.visible("ワンハンド・ダンベルロー")
                runBlocking {
                    val active=controller.resumeActive()!!;ids.add(active.session.id)
                    assertEquals("home.B",active.session.routineKey)
                    assertEquals(SessionStatus.COMPLETED,db.workoutSessionDao().session(old)!!.status)
                    val oldExercise=db.workoutSessionDao().exercises(old).first()
                    assertEquals(20.0,db.workoutSessionDao().records(oldExercise.id).single().weightKg!!,0.0)
                }
                actions.click("中断して戻る")
                scenario.recreate()
                assertFalse(device.hasObject(By.textStartsWith("表示中：")))
                actions.click("トレーニングを再開")
                actions.visible("ワンハンド・ダンベルロー")
            }
        } finally { runBlocking { ids.forEach { db.openHelper.writableDatabase.execSQL("DELETE FROM workout_sessions WHERE id=?",arrayOf(it)) } } }
    }
}
