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
class SetEditingFlowTest {
    @Test fun editFirstOfThreeSetsWithoutMovingItAndRestoreAfterRecreation() {
        val db = AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        val controller = SessionController(db)
        var session: Long? = null
        try {
            val before = runBlocking {
                LegacyUiFixture.ensure()
                session = controller.start(1)
                val exercise = controller.resumeActive()!!.exercises.first().exercise.id
                repeat(3) { controller.completeSet(exercise, 20.0 + it * 2.5, 10 - it) }
                controller.pause(session!!)
                db.workoutSessionDao().records(exercise)
            }
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val actions = UiActions(device)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                actions.click("中断中のメニューを再開")
                actions.visible("チェストプレス")
                assertFalse(device.hasObject(By.text("メイン：大胸筋")))
                assertFalse(device.hasObject(By.textStartsWith("前回値は提案です。")))
                actions.click(By.desc("1セット目を編集"))
                actions.visible("1セット目を編集")
                actions.setText(By.clazz("android.widget.EditText").text("20.0"), "19")
                actions.click("キャンセル")
                runBlocking { assertEquals(before, db.workoutSessionDao().records(before.first().sessionExerciseId)) }
                actions.click(By.desc("1セット目を編集"))
                actions.setText(By.clazz("android.widget.EditText").text("20.0"), "0")
                actions.setText(By.clazz("android.widget.EditText").text("10"), "12")
                actions.click("変更を保存")
                actions.visible("重量は0より大きい数値、回数は1以上の整数を入力してください。")
                runBlocking { assertEquals(before, db.workoutSessionDao().records(before.first().sessionExerciseId)) }
                actions.setText(By.clazz("android.widget.EditText").text("0"), "17.5")
                actions.click("変更を保存")
                actions.visible("17.5 kg × 12回")
                actions.visible("4セット完了")
                val after = runBlocking { db.workoutSessionDao().records(before.first().sessionExerciseId) }
                assertEquals(before.first().copy(weightKg = 17.5, reps = 12), after.first())
                assertEquals(before.drop(1), after.drop(1))
                scenario.recreate()
                actions.visible("17.5 kg × 12回")
                actions.visible("4セット完了")
            }
        } finally { session?.let { id -> runBlocking {
            controller.resume(id)
            for (exercise in db.workoutSessionDao().exercises(id)) {
                for (record in db.workoutSessionDao().records(exercise.id)) controller.undoSet(record.id)
            }
            controller.finish(id)
        } } }
    }
}
