package com.myworkout

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.After
import kotlinx.coroutines.runBlocking
import com.myworkout.domain.SessionController
import com.myworkout.data.AppDatabase
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDesignTest {
    private var created:Long?=null
    @After fun cleanup() {
        created?.let { id -> runBlocking { SessionController(AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)).finish(id) } }
    }

    @Test fun steppersPersistentArtworkRapidTapAndRealSummary() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val actions=UiActions(device)
        fun click(text:String) = actions.click(text)
        ActivityScenario.launch(MainActivity::class.java).use {
            click("トレーニング開始")
            assertNotNull(device.wait(Until.findObject(By.descStartsWith("マシン画像")),10_000))
            created=runBlocking { SessionController(AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)).resumeActive()!!.session.id }
            assertTrue(device.hasObject(By.descContains("筋肉図")))
            actions.click(By.desc("重量を増やす"))
            actions.click(By.desc("回数を増やす"))
            val save = device.findObject(By.text("セット完了")).visibleCenter
            assertTrue(device.click(save.x,save.y))
            assertTrue(device.click(save.x,save.y))
            actions.visible("記録済み：1セット")
            val count=runBlocking { SessionController(AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)).resumeActive()!!.exercises.first().sets.size }
            assertEquals("Rapid save count",1,count)
            actions.visible("記録済み：1セット")
            assertFalse(device.hasObject(By.text("記録済み：2セット")))
            assertTrue(device.hasObject(By.descContains("筋肉図")))
            val scroll = device.findObject(By.scrollable(true))
            scroll?.scroll(Direction.DOWN,0.8f)
            assertTrue(device.hasObject(By.descContains("筋肉図")))
            click("トレーニングを終了")
            click("終了する")
            assertNotNull(device.wait(Until.findObject(By.desc("実施種目数：1")),10_000))
            assertTrue(device.hasObject(By.desc("完了セット数：1")))
            assertTrue(device.hasObject(By.text("前回の比較記録はありません。")))
            click("ホームへ")
        }
    }
}
