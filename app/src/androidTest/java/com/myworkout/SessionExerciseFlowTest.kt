package com.myworkout

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import java.io.ByteArrayOutputStream
import android.util.Log
import org.junit.Test
import org.junit.After
import com.myworkout.data.AppDatabase
import com.myworkout.domain.SessionController
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionExerciseFlowTest {
    private var createdSessionId: Long? = null
    @After fun finishTestSession() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        createdSessionId?.let { id -> runBlocking { SessionController(AppDatabase.getInstance(context)).finish(id) } }
    }

    @Test fun reorderSkipAddRestoreAndResumeQueue() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val actions=UiActions(device)
        fun click(selector:BySelector) = actions.click(selector)
        fun clickText(text:String) = actions.click(text)
        fun visible(text:String) = actions.visible(text)
        LegacyUiFixture.ensure()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            clickText("マシントレーニング")
            clickText("トレーニング開始")
            visible("チェストプレス")
            createdSessionId = runBlocking {
                SessionController(AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)).resumeActive()!!.session.id
            }
            clickText("当日の種目を編集")
            click(By.desc("2番目のラットプルダウンを上へ"))
            visible("1. ラットプルダウン")
            clickText("閉じる")
            visible("2 / 5種目")
            visible("チェストプレス")
            clickText("当日の種目を編集")
            click(By.desc("2番目のチェストプレスをスキップ"))
            visible("スキップ（未実施）")
            clickText("閉じる")
            visible("ショルダープレス")
            clickText("当日の種目を編集")
            clickText("種目を追加")
            assertTrue(UiScrollable(UiSelector().scrollable(true)).scrollTextIntoView("プリチャーカール"))
            clickText("プリチャーカール")
            clickText("閉じる")
            visible("3 / 6種目")
            clickText("中断して戻る")
            scenario.recreate()
            clickText("トレーニングを再開")
            visible("ショルダープレス")
            visible("3 / 6種目")
            clickText("当日の種目を編集")
            visible("スキップ（未実施）")
            click(By.desc("2番目のチェストプレスのスキップを解除"))
            clickText("2. チェストプレス")
            visible("チェストプレス")
            click(By.desc("重量を増やす"))
            clickText("1セット完了")
            visible("記録済み：1セット")
            clickText("取り消し")
            clickText("トレーニングを終了")
            clickText("終了する")
            visible("セットが未記録のため、記録を残さず終了しました。")
        }
    }
}
