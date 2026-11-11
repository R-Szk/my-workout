package com.myworkout

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.myworkout.data.*
import com.myworkout.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseStepImagesTest {
    @Test fun allEighteenResourcesRetainTheirDimensionsAndTransparency() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val dimensions = List(6) { List(3) { 900 to 900 } }
        ExerciseSteps.all.forEachIndexed { index, item -> item.images.forEachIndexed { step, id ->
            val bitmap = requireNotNull(BitmapFactory.decodeResource(resources,id))
            try {
                assertEquals(item.id, dimensions[index][step], bitmap.width to bitmap.height)
                assertTrue(bitmap.hasAlpha())
                assertEquals("Transparent corner ${item.id} step $step",0,Color.alpha(bitmap.getPixel(0,0)))
            } finally { bitmap.recycle() }
        } }
    }
    @Test fun sixGuidesShowOrderedStaticImagesAndKeepMuscleMap() {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());val actions=UiActions(device)
        ActivityScenario.launch(MainActivity::class.java).use {
            actions.click("動作を見る")
            ExerciseSteps.all.forEachIndexed { index,item ->
                actions.visible(item.name)
                val first=By.desc("${item.name} ステップ1")
                actions.find(first)
                device.waitForIdle()
                android.os.SystemClock.sleep(250) // Capture the committed frame after changing exercises.
                val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
                if (output != null && InstrumentationRegistry.getArguments().getString("captureScreenshots")=="true") device.takeScreenshot(java.io.File(output, "steps-${item.id}.png"))
                assertTrue(device.hasObject(By.descContains("筋肉図")))
                assertTrue(device.wait(Until.hasObject(By.desc("${item.name} ステップ3")), 6000))
                if(index<5) actions.click("次の種目")
            }
            actions.click("戻る")
            actions.visible("トレーニング開始")
        }
    }
    @Test fun pingPongOrderAndCustomIntervalPersistAfterRecreation() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val preferences=context.getSharedPreferences("exercise_image_playback",android.content.Context.MODE_PRIVATE)
        val old=preferences.all["interval_ms"] as? Long
        preferences.edit().putLong("interval_ms",1000).commit()
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());val actions=UiActions(device)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                actions.click("動作を見る")
                actions.visible("表示時間：1秒")
                var bounds:android.graphics.Rect?=null
                for (step in listOf(1,2,3,2,1)) {
                    val image=device.wait(Until.findObject(By.desc("プッシュアップ ステップ$step")),6000)
                    assertNotNull("Missing playback step $step",image)
                    if(bounds==null) bounds=image.visibleBounds else assertEquals(bounds,image.visibleBounds)
                    assertEquals(1,device.findObjects(By.descStartsWith("プッシュアップ ステップ")).size)
                }
                actions.click("表示時間：1秒")
                actions.setText(By.clazz("android.widget.EditText").text("1"),"0")
                actions.click("適用")
                actions.visible("0.1〜60秒で入力してください。")
                actions.setText(By.clazz("android.widget.EditText").text("0"),"0.5")
                actions.click("適用")
                actions.visible("表示時間：0.5秒")
                assertEquals(500L,preferences.getLong("interval_ms",0))
                scenario.recreate()
                actions.visible("表示時間：0.5秒")
                val output=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
                if(output!=null && InstrumentationRegistry.getArguments().getString("captureScreenshots")=="true") device.takeScreenshot(java.io.File(output,"steps-pingpong.png"))
            }
        } finally {
            if(old==null) preferences.edit().remove("interval_ms").commit() else preferences.edit().putLong("interval_ms",old).commit()
        }
    }
    @Test fun compactGuideKeepsImagesMusclesAndNavigationVisible() {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());val actions=UiActions(device)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                com.myworkout.ui.theme.WorkoutTheme {
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                        Box(Modifier.width(320.dp).height(300.dp)) {
                            com.myworkout.feature.guide.ExerciseGuideScreen { }
                        }
                    }
                }
            } }
            val first=actions.find(By.desc("プッシュアップ ステップ1"))
            assertTrue(first.visibleBounds.height()>30)
            assertTrue(device.hasObject(By.descContains("筋肉図")))
            actions.click("次の種目")
            actions.find(By.desc("ダンベル・サイドレイズ ステップ1"))
        }
    }
    @Test fun recordScreenCanShowStepsWithoutChangingTheSaveFlow() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=AppDatabase.getInstance(context);val controller=SessionController(db)
        var session:Long?=null;var machine:Long?=null
        try {
            val exercise=runBlocking {
                LegacyUiFixture.ensure()
                val id=(db.workoutDao().observeMachines().first().maxOfOrNull { it.id } ?: 0)+1
                machine=id
                db.workoutDao().insertMachines(listOf(Machine(id,"ダンベルカール",visualKey="home.dumbbell_curl")))
                session=controller.start(1)
                val added=SessionExerciseEditor(db).add(session!!,id)
                controller.moveTo(session!!,controller.resumeActive()!!.exercises.lastIndex)
                controller.pause(session!!)
                added
            }
            val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());val actions=UiActions(device)
            ActivityScenario.launch(MainActivity::class.java).use {
                actions.click("中断中のメニューを再開")
                actions.find(By.desc("ダンベルカール ステップ1"))
                assertTrue(device.hasObject(By.descContains("筋肉図")))
                actions.setText(By.clazz("android.widget.EditText").text("0"),"5")
                actions.click("1セット完了")
                actions.visible("記録済み：1セット")
                actions.visible("2セット完了")
                runBlocking {
                    val record=db.workoutSessionDao().records(exercise).single()
                    assertEquals(5.0,record.weightKg!!, 0.0)
                    assertEquals(10,record.reps)
                    controller.undoSet(record.id)
                }
            }
        } finally { runBlocking {
            session?.let { controller.finish(it) }
            machine?.let { db.openHelper.writableDatabase.execSQL("DELETE FROM machines WHERE id = ?", arrayOf(it)) }
        } }
    }
}
