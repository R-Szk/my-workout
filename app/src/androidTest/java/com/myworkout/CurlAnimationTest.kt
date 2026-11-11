package com.myworkout

import android.graphics.Rect
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.myworkout.feature.animation.CurlPrototypeActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurlAnimationTest {
    private fun signature(device: UiDevice, bounds: Rect): IntArray {
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        return try {
            val samples = mutableListOf<Int>()
            for (y in bounds.top + 4 until bounds.bottom - 4 step 5)
                for (x in bounds.left + 4 until bounds.right - 4 step 5) samples.add(bitmap.getPixel(x, y))
            samples.toIntArray()
        } finally { bitmap.recycle() }
    }
    @Test fun autoPlayTapPauseResumeAndPausedRecreation() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val actions = UiActions(device)
        ActivityScenario.launch(CurlPrototypeActivity::class.java).use { scenario ->
            val animation = actions.find(By.desc("カール動作アニメーション"))
            assertTrue(animation.visibleBounds.height() > 100)
            assertTrue(device.hasObject(By.text("力こぶ")))
            actions.visible("一時停止")
            val bounds = animation.visibleBounds
            val first = signature(device, bounds)
            SystemClock.sleep(400)
            assertFalse("Autoplay must change the illustration", first.contentEquals(signature(device, bounds)))
            actions.click(By.desc("カール動作アニメーション"))
            actions.visible("再生する")
            SystemClock.sleep(450) // Let the tap ripple finish before comparing the actual drawing.
            val paused = signature(device, bounds)
            SystemClock.sleep(450)
            assertArrayEquals("Paused illustration must remain still", paused, signature(device, bounds))
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            actions.visible("再生する")
            scenario.recreate()
            actions.visible("再生する")
            val restoredBounds = actions.find(By.desc("カール動作アニメーション")).visibleBounds
            SystemClock.sleep(150)
            assertArrayEquals("Recreation must preserve the paused pose", paused, signature(device, restoredBounds))
            actions.click("再生する")
            actions.visible("一時停止")
            val resumed = signature(device, restoredBounds)
            SystemClock.sleep(400)
            assertFalse(resumed.contentEquals(signature(device, restoredBounds)))
        }
    }
    @Test fun compactWindowKeepsArtworkMapAndControlsReachable() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val actions = UiActions(device)
        ActivityScenario.launch(CurlPrototypeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    com.myworkout.ui.theme.WorkoutTheme {
                        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                            Box(Modifier.width(320.dp).height(300.dp)) {
                                com.myworkout.feature.animation.CurlPrototypeScreen(activity::finish)
                            }
                        }
                    }
                }
            }
            actions.find(By.desc("カール動作アニメーション"))
            actions.visible("力こぶ")
            actions.click("一時停止")
            actions.visible("再生する")
            actions.click("フォームのコツ")
            actions.visible("フォームのコツ")
            actions.click("閉じる")
            actions.visible("再生する")
        }
    }
}
