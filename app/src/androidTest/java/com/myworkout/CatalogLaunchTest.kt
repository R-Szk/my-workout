package com.myworkout

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogLaunchTest {
    @Test fun launchShowsBothMenusFromLocalDatabase() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        ActivityScenario.launch(MainActivity::class.java).use {
            assertTrue(device.wait(Until.hasObject(By.text("A：胸・肩・二の腕")), 10_000))
            device.findObject(By.text("B：背中・力こぶ・下半身")).click()
            assertTrue(device.wait(Until.hasObject(By.textContains("ワンハンド・ダンベルロー")), 5_000))
        }
    }
}
