package com.myworkout

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.myworkout.data.*
import com.myworkout.domain.SessionController
import com.myworkout.feature.workout.*
import com.myworkout.ui.components.MuscleMap
import com.myworkout.ui.theme.WorkoutTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompactArtworkTest {
    @Test fun rowDiagramHasVisiblePrimaryAndSecondaryRegions() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val device=UiDevice.getInstance(instrumentation)
        val profile=VisualCatalog.find("seated_row_plate_loaded")!!
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent { WorkoutTheme { MuscleMap(profile,Modifier.size(280.dp,240.dp)) } } }
            val diagram = if(profile.useAssetMuscleImage) device.wait(Until.findObject(By.desc(profile.label+"の筋肉図")),10000)
                else device.wait(Until.findObject(By.descContains("正面と背面")),10000)
            assertNotNull(diagram)
            device.waitForIdle()
            val bounds=diagram!!.visibleBounds
            val bitmap=instrumentation.uiAutomation.takeScreenshot()!!
            var red=0;var orange=0
            for(y in bounds.top until bounds.bottom) for(x in bounds.left until bounds.right) {
                val pixel=bitmap.getPixel(x,y);val r=android.graphics.Color.red(pixel);val g=android.graphics.Color.green(pixel);val b=android.graphics.Color.blue(pixel)
                if(r>200 && g<125 && b<125) red++
                if(r>200 && g in 130..190 && b<125) orange++
            }
            bitmap.recycle()
            assertTrue("Primary red is missing",red>10)
            assertTrue("Secondary orange is missing",orange>10)
        }
    }

    @Test fun shortWindowKeepsFieldsAndSaveReachableWithArtworkPresent() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val controller=SessionController(AppDatabase.getInstance(context))
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        var created:Long?=null
        try {
            LegacyUiFixture.ensure()
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val actions=UiActions(device)
                actions.click("マシントレーニング")
                actions.click("トレーニング開始")
                actions.visible("チェストプレス")
                created=runBlocking { controller.resumeActive()!!.session.id }
                scenario.onActivity { activity ->
                    val model=ViewModelProvider(activity)[WorkoutViewModel::class.java]
                    activity.setContent {
                        WorkoutTheme {
                            val state by model.state.collectAsState()
                            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                                Box(Modifier.fillMaxWidth().height(230.dp)) { WorkoutScreen(state,model) }
                            }
                        }
                    }
                }
                actions.find(By.textContains("記録0セット"))
                val field=actions.find(By.clazz("android.widget.EditText"))
                assertTrue(field.visibleBounds.height()>0)
                field.text="5"
                assertTrue(device.hasObject(By.descContains("筋肉図")))
                actions.click("1セット完了")
                actions.find(By.textContains("記録1セット"))
                val record=runBlocking { controller.resumeActive()!!.exercises.first().sets.single() }
                assertEquals(5.0,record.weightKg!!, 0.0)
                runBlocking { controller.undoSet(record.id) }
            }
        } finally { created?.let { runBlocking { controller.finish(it) } } }
    }
}
