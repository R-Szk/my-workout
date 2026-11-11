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
class EmptyWorkoutFlowTest {
    @Test fun finishingWithoutSetsReturnsHomeWithoutCompletionOrHistory() {
        val db=AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        val before=runBlocking { WorkoutHistoryRepository(db).observeHistory().first().map{it.session.id} }
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());val actions=UiActions(device)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            actions.click("トレーニング開始")
            actions.visible("プッシュアップ")
            actions.click("トレーニングを終了")
            actions.click("終了する")
            actions.visible("セットが未記録のため、記録を残さず終了しました。")
            actions.visible("トレーニング開始")
            assertFalse(device.hasObject(By.text("トレーニング完了")))
            scenario.recreate()
            actions.visible("トレーニング開始")
            runBlocking {
                assertNull(SessionController(db).resumeActive())
                assertEquals(before,WorkoutHistoryRepository(db).observeHistory().first().map{it.session.id})
            }
        }
    }
}
