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
class HomeRecordingFlowTest {
    @Test fun recordBodyweightEditResumeAndFinishOneExercise() {
        val db=AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        val controller=SessionController(db)
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val actions=UiActions(device)
        var created:Long?=null
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                actions.click("トレーニング開始")
                actions.visible("プッシュアップ")
                actions.find(By.descStartsWith("プッシュアップ ステップ"))
                assertTrue(device.hasObject(By.descContains("筋肉図")))
                actions.find(By.clazz("android.widget.EditText"))
                assertEquals(1,device.findObjects(By.clazz("android.widget.EditText")).size)
                actions.setText(By.clazz("android.widget.EditText"),"8")
                actions.click("1セット完了")
                actions.visible("2セット完了")
                created=runBlocking { controller.resumeActive()!!.session.id }
                actions.click(By.desc("1セット目を編集"))
                // Query within the edit dialog: Android may expose the background screen too.
                var dialog=actions.find(By.text("変更を保存"))
                while(dialog.findObjects(By.clazz("android.widget.EditText")).isEmpty()) dialog=requireNotNull(dialog.parent)
                val editFields=dialog.findObjects(By.clazz("android.widget.EditText"))
                assertEquals(1,editFields.size)
                editFields.single().text="10"
                actions.click("変更を保存")
                actions.visible("10回")
                actions.click("中断して戻る")
                scenario.recreate()
                actions.click("トレーニングを再開")
                actions.visible("10回")
                actions.click("トレーニングを終了")
                actions.click("終了する")
                actions.find(By.desc("実施種目数：1"))
                actions.find(By.desc("完了セット数：1"))
                actions.visible("今回 10回")
                assertFalse(device.hasObject(By.textContains("null")))
                runBlocking {
                    val sets=db.workoutSessionDao().exercises(created!!).flatMap { db.workoutSessionDao().records(it.id) }
                    assertEquals(1,sets.size);assertEquals(10,sets.single().reps);assertNull(sets.single().weightKg)
                }
            }
        } finally { created?.let { id -> runBlocking { db.openHelper.writableDatabase.execSQL("DELETE FROM workout_sessions WHERE id=?",arrayOf(id)) } } }
    }
    @Test fun guideStartsCurlWithPositiveDumbbellWeightAndPreservesActiveSession() {
        val db=AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        val controller=SessionController(db)
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());val actions=UiActions(device)
        var created:Long?=null
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                actions.click("動作を見る")
                repeat(4) { actions.click("次の種目") }
                actions.visible("ダンベルカール")
                actions.click("この種目から記録を始める")
                actions.visible("ダンベルカール")
                actions.find(By.clazz("android.widget.EditText"))
                created=runBlocking { controller.resumeActive()!!.session.id }
                actions.click("1セット完了")
                actions.visible("重量は0より大きい数値、回数は1以上の整数を入力してください。")
                actions.setText(By.clazz("android.widget.EditText").text("0"),"5")
                actions.click("1セット完了")
                actions.visible("2セット完了")
                actions.click("中断して戻る")
                actions.click("動作を見る")
                actions.click("トレーニングを再開")
                actions.visible("ダンベルカール")
                runBlocking {
                    val active=controller.resumeActive()!!
                    assertEquals(created,active.session.id)
                    assertEquals(1,active.session.currentPosition)
                    assertEquals(5.0,active.exercises[1].sets.single().weightKg!!,0.0)
                }
            }
        } finally { created?.let { id -> runBlocking { db.openHelper.writableDatabase.execSQL("DELETE FROM workout_sessions WHERE id=?",arrayOf(id)) } } }
    }
}
