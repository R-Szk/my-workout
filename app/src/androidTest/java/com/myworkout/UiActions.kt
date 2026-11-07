package com.myworkout

import androidx.test.uiautomator.*
import org.junit.Assert.assertNotNull

internal class UiActions(private val device: UiDevice) {
    fun find(selector: BySelector): UiObject2 {
        device.wait(Until.findObject(selector),3000)?.let { return it }
        for (direction in listOf(Direction.DOWN,Direction.UP)) repeat(5) {
            val scroll = device.findObject(By.scrollable(true)) ?: return@repeat
            scroll.scroll(direction,.7f)
            device.wait(Until.findObject(selector),500)?.let { return it }
        }
        val item=device.wait(Until.findObject(selector),2000)
        if(item==null) {
            device.executeShellCommand("screencap -p /sdcard/myworkout-ui-failure-${selector.toString().hashCode()}.png")
            val out=java.io.ByteArrayOutputStream();device.dumpWindowHierarchy(out)
            android.util.Log.e("WorkoutUiFailure",out.toString())
        }
        assertNotNull("Missing: $selector",item)
        return item!!
    }
    fun click(text:String) = click(By.text(text))
    fun click(selector:BySelector) { find(selector).click();device.waitForIdle() }
    fun visible(text:String) { find(By.text(text)) }
}
