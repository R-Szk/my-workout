package com.myworkout

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherTest {
    @Test fun onlyMainActivityAppearsInLauncher() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val entries=context.packageManager.queryIntentActivities(intent,PackageManager.ResolveInfoFlags.of(0))
        assertEquals(listOf(MainActivity::class.java.name),entries.map { it.activityInfo.name })
        assertEquals(MainActivity::class.java.name,context.packageManager.getLaunchIntentForPackage(context.packageName)?.component?.className)
    }
}
