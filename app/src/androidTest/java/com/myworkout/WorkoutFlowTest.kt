package com.myworkout

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutFlowTest {
    @Test fun savePauseRecreateResumeUndoAndFinish() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        LegacyUiFixture.ensure()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val actions = UiActions(device)
            fun click(text:String) = actions.click(text)
            click("マシントレーニング")
            click("トレーニング開始")
            assertTrue(device.wait(Until.hasObject(By.text("チェストプレス")), 10_000))
            val fields = device.findObjects(By.clazz("android.widget.EditText"))
            assertEquals(2, fields.size)
            fields[0].text = "0.0"
            fields[1].text = "10"
            click("1セット完了")
            assertTrue(device.wait(Until.hasObject(By.text("重量は0より大きい数値、回数は1以上の整数を入力してください。")), 5_000))
            assertTrue(device.hasObject(By.text("記録済み：0セット")))
            val validFields = device.findObjects(By.clazz("android.widget.EditText"))
            validFields[0].text = "25"
            validFields[1].text = "10"
            click("1セット完了")
            actions.visible("記録済み：1セット")
            actions.visible("2セット完了")
            click("次の種目")
            assertTrue(device.wait(Until.hasObject(By.text("ラットプルダウン")), 5_000))
            click("中断して戻る")
            scenario.recreate()
            click("トレーニングを再開")
            assertTrue(device.wait(Until.hasObject(By.text("ラットプルダウン")), 5_000))
            click("前の種目")
            actions.visible("25.0 kg × 10回")
            click("取り消し")
            actions.visible("記録済み：0セット")
            actions.visible("1セット完了")
            click("トレーニングを終了")
            click("終了する")
            assertTrue(device.wait(Until.hasObject(By.text("セットが未記録のため、記録を残さず終了しました。")), 5_000))
            actions.visible("トレーニング開始")
        }
    }
}
